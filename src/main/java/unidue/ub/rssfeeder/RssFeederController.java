package unidue.ub.rssfeeder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import unidue.ub.media.blacklist.Ignored;
import unidue.ub.media.analysis.Nrequests;
import unidue.ub.settings.fachref.Alertcontrol;
import unidue.ub.settings.fachref.Notationgroup;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.*;

@RestController
public class RssFeederController {

    //@Value("${ub.statistics.settings.url}")
    private String settingsUrl = "http://localhost:11300";

    //@Value("${ub.statistics.data.url}")
    private String dataUrl = "http://localhost:11200";

    private Hashtable<String, Nrequests> foundNrequests;

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private ObjectMapper mapper = new ObjectMapper();
    private static final HttpConnectionManager httpConnectionManager = new MultiThreadedHttpConnectionManager();

    @ResponseBody
    @RequestMapping("/")
    public String getNrequestsFeed(@RequestParam("alertcontrol") String identifier, @RequestParam("requestor") Optional<String> requestor) throws FeedException, URISyntaxException, UnsupportedEncodingException {
        foundNrequests = new Hashtable<>();

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");
        feed.setTitle("Hitlisten-Feed");
        feed.setDescription("Titel, die die eingestellten Schwellenwerte überschreiten.");

        ResponseEntity<Alertcontrol> responseAlertcontrol = new RestTemplate().getForEntity(
                settingsUrl + "/alertcontrol/" + identifier,
                Alertcontrol.class
        );
        Alertcontrol alertcontrol = responseAlertcontrol.getBody();

        ResponseEntity<Notationgroup> responseNotationgroup = new RestTemplate().getForEntity(
                settingsUrl + "/notationgroup/" + alertcontrol.getNotationgroup(),
                Notationgroup.class
        );
        Notationgroup notationgroup = responseNotationgroup.getBody();

        String url = dataUrl + "/nrequests/getForTimeperiod"
                + "?startNotation=" + notationgroup.getNotationsStart() + "&endNotation=" + notationgroup.getNotationsEnd()
                + "&timeperiod=" + alertcontrol.getTimeperiod();

        Nrequests[] nrequestss = new RestTemplate().getForEntity(url, Nrequests[].class).getBody();

        for (Nrequests nrequests : nrequestss) {
            try {
                ResponseEntity<Ignored> response = new RestTemplate().getForEntity(
                        "http://localhost:8082/api/blacklist/ignored/" + nrequests.getTitleId(),
                        Ignored.class,
                        0);
                Ignored ignored = response.getBody();
                if (ignored.getExpire().after(new Date()) && ignored.getType().equals("eventanalysis")) {
                    log.info("manifestion " + nrequests.getTitleId() + " is blacklisted");
                    continue;
                }
            } catch (HttpClientErrorException httpClientErrorException) {
                if (requestor.isPresent()) {
                    if (nrequests.status == null || nrequests.status.equals("") || nrequests.status.equals("NEW")) {
                        nrequests.status = requestor.get();
                    } else if (nrequests.status.contains(requestor.get()))
                        continue;
                } else {
                    nrequests.status = nrequests.status + " " + requestor.get();
                }

                boolean isTotalDurationThresholdExceeded = nrequests.geTotalDuration() > alertcontrol.getThresholdDuration();
                boolean isRatioThresholdExceeded = nrequests.getRatio() > alertcontrol.getThresholdRatio();
                boolean isNRequestsThresholdExceeded = nrequests.getNRequests() > alertcontrol.getThresholdRequests();
                if (isTotalDurationThresholdExceeded || isRatioThresholdExceeded || isNRequestsThresholdExceeded) {
                    if (foundNrequests.containsKey(nrequests.getTitleId())) {
                        if (foundNrequests.get(nrequests.getTitleId()).getNRequests() < nrequests.getNRequests())
                            foundNrequests.replace(nrequests.getTitleId(), nrequests);
                    } else {
                        foundNrequests.put(nrequests.getTitleId(),nrequests);
                    }
                }
            }
        }
        feed.setEntries(getFeedListFromNrequests());
        updateNrequests(nrequestss);
        return new SyndFeedOutput().outputString(feed);
    }

    private List<SyndEntry> getFeedListFromNrequests() {
        List<SyndEntry> entries = new ArrayList<>();
        for (Nrequests nrequests : foundNrequests.values()) {
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(nrequests.getShelfmark());
            entry.setUpdatedDate(new Date());
            SyndContent content = new SyndContentImpl();
            content.setValue("<h2>" + nrequests.getShelfmark() + ":</h2>" +
                    nrequests.getMab() + " <br /> " +
                    "<table><tr><td>Verhältnis:</td><td>" + String.format("%.2f", nrequests.getRatio()) + "</td></tr><tr><td>Anzahl Vormerkungen: </td><td>" + nrequests.getNRequests() +
                    " </td></tr><tr><td>Dauer: </td><td>" + nrequests.geTotalDuration() +
                    " </td></tr><table><br /><a href=\"/protokoll?shelfmark=" + nrequests.getShelfmark() + "&exact=\">Zum Ausleihprotokoll</a>");
            entry.setContents(Collections.singletonList(content));
            entries.add(entry);
        }
        return entries;
    }

    private void updateNrequests(Nrequests[] nrequestss) {
        int succesfullPosts = 0;
        for (Nrequests nrequest : nrequestss) {
            try {
                String json = mapper.writeValueAsString(nrequest);
                HttpClient client = new HttpClient(httpConnectionManager);
                PostMethod post = new PostMethod("http://localhost:8082/api/data/nrequests");
                RequestEntity entity = new StringRequestEntity(json, "application/json", null);
                post.setRequestEntity(entity);
                int status = client.executeMethod(post);
                if (status == 201)
                    succesfullPosts++;
                post.releaseConnection();
            } catch (IOException ioEx) {
                log.warn("could not update nrequests for " + nrequest.getTitleId());
            }
        }
        log.info("successfully posted " + succesfullPosts + " of " + nrequestss.length + " nrequests.");
    }

}
