package unidue.ub.rssfeeder;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import unidue.ub.media.analysis.Nrequests;
import unidue.ub.settings.fachref.Alertcontrol;
import unidue.ub.settings.fachref.Notationgroup;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@RestController
public class RssFeederController {

    //@Value("${ub.statistics.settings.url}")
    String settingsUrl = "http://localhost:11300";

    //@Value("${ub.statistics.data.url}")
    String dataUrl = "http://localhost:11200";

    private final static long DAYSINMILLIS = 24* 60 * 60 * 1000;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @ResponseBody
    @RequestMapping("/")
    public String getNrequestsFeed(@RequestParam("alertcontrol") String identifier) throws FeedException, URISyntaxException, UnsupportedEncodingException {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");
        feed.setTitle("Hitlisten-Feed");
        feed.setDescription("Titel, die die eingestellten Schwellenwerte überschreiten.");

        ResponseEntity<Alertcontrol> responseAlertcontrol = new RestTemplate().getForEntity(
                settingsUrl + "/alertcontrol/" + identifier,
                Alertcontrol.class
        );
        Alertcontrol alertcontrol = responseAlertcontrol.getBody();
        log.info("retrieved " + alertcontrol.toString());

        ResponseEntity<Notationgroup> responseNotationgroup = new RestTemplate().getForEntity(
                settingsUrl + "/notationgroup/" + alertcontrol.getNotationgroup(),
                Notationgroup.class
        );
        Notationgroup notationgroup = responseNotationgroup.getBody();

        log.info("retrieved " + notationgroup.getNotationgroupName());
        LocalDateTime startDate = LocalDateTime.now().minusDays(alertcontrol.getTimeperiod());
        ZonedDateTime zonedStartDate = startDate.atZone(ZoneId.systemDefault());
        Date date = Date.from(zonedStartDate.toInstant());


        String url = dataUrl + "/nrequests/search/getFilteredNrequestsForNotationgroup"
                + "?startNotation=" + notationgroup.getNotationsStart() + "&endNotation=" + notationgroup.getNotationsEnd()
                + "&ratio=" + alertcontrol.getThresholdRatio()
                + "&duration=" + alertcontrol.getThresholdDuration()
                + "&requests=" + alertcontrol.getThresholdRequests()
                + "&startDate=" + URLEncoder.encode(date.toString(),"UTF-8");
        log.info("querying " + url);
        Traverson traverson = new  Traverson(new URI(url), MediaTypes.HAL_JSON);
        Traverson.TraversalBuilder tb = traverson.follow("$._links.self.href");
        ParameterizedTypeReference<Resources<Nrequests>> typeRefDevices = new ParameterizedTypeReference<Resources<Nrequests>>() {};
        Resources<Nrequests> resourcesNrequests = tb.toObject(typeRefDevices);
        Collection<Nrequests> nrequestss = resourcesNrequests.getContent();
        log.info("found " + nrequestss.size() + " entries");

        List entries = new ArrayList();

        for (Nrequests nrequests : nrequestss) {
            boolean isTotalDurationThresholdExceeded = nrequests.geTotalDuration() > alertcontrol.getThresholdDuration();
            boolean isRatioThresholdExceeded = nrequests.getRatio() > alertcontrol.getThresholdRatio();
            boolean isNRequestsThresholdExceeded = nrequests.getNRequests() > alertcontrol.getThresholdRequests();
            if (isTotalDurationThresholdExceeded|| isRatioThresholdExceeded || isNRequestsThresholdExceeded) {
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(nrequests.getShelfmark());
            entry.setUpdatedDate(new Date());
            SyndContent content = new SyndContentImpl();
            content.setValue(nrequests.getMab() + "\n" +
                    "Verhältnis: " + nrequests.getRatio() + "\n Anzahl Vormerkungen: " + nrequests.getNRequests() +
                    "\n Dauer: " + nrequests.geTotalDuration() );
            entry.setContents(Collections.singletonList(content));
            entries.add(entry);
            }
        }
        feed.setEntries(entries);
        return new SyndFeedOutput().outputString(feed);
    }

}
