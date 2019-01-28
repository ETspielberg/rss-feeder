package unidue.ub.rssfeeder;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import unidue.ub.rssfeeder.clients.BlacklistClient;
import unidue.ub.rssfeeder.clients.SettingsClient;
import unidue.ub.rssfeeder.clients.StockanalyzerClient;
import unidue.ub.rssfeeder.model.Alertcontrol;
import unidue.ub.rssfeeder.model.Notationgroup;
import unidue.ub.rssfeeder.model.Nrequests;

import java.util.*;

@Controller
public class RssFeederController {

    private final BlacklistClient blacklistClient;

    private final SettingsClient settingsClient;

    private final StockanalyzerClient stockanalyzerClient;

    private Hashtable<String, Nrequests> foundNrequests;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public RssFeederController(BlacklistClient blacklistClient, SettingsClient settingsClient, StockanalyzerClient stockanalyzerClient) {
        this.blacklistClient = blacklistClient;
        this.settingsClient = settingsClient;
        this.stockanalyzerClient = stockanalyzerClient;
    }


    @ResponseBody
    @RequestMapping("/")
    public String getNrequestsFeed(@RequestParam("alertcontrol") String identifier, @RequestParam("requestor") Optional<String> requestor) throws FeedException {
        foundNrequests = new Hashtable<>();

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");
        feed.setTitle("Hitlisten-Feed");
        feed.setDescription("Titel, die die eingestellten Schwellenwerte überschreiten.");

        Alertcontrol alertcontrol = stockanalyzerClient.getAlertcontrol(identifier).getContent();
        Notationgroup notationgroup = settingsClient.getNotationgroup(alertcontrol.getNotationgroup()).getContent();
        List<Nrequests> nrequestss = stockanalyzerClient.getNrequestsForTimeperiod(notationgroup.getNotationsStart(), notationgroup.getNotationsEnd(), alertcontrol.getTimeperiod());

        for (Nrequests nrequests : nrequestss) {
            boolean isBlocked = blacklistClient.isBlocked(nrequests.getIdentifier(), "nrequests");
            if (isBlocked) {
                log.info("manifestion " + nrequests.getTitleId() + " is blacklisted");
                continue;
            }
            if (requestor.isPresent()) {
                log.info("checking for requestor " + requestor.get());
                if (nrequests.getStatus() == null || nrequests.getStatus().equals("") || nrequests.getStatus().equals("NEW")) {
                    nrequests.setStatus(requestor.get());
                    log.info("set status to requestor");
                } else if (nrequests.getStatus().contains(requestor.get())) {
                    log.info("nrequests already collected by requestor");
                    continue;
                } else {
                    nrequests.setStatus(nrequests.getStatus() + " " + requestor.get());
                    log.info("adding requestor " + requestor.get() + " to list of requestors: " + nrequests.getStatus());
                }
            }

            boolean isTotalDurationThresholdExceeded = nrequests.geTotalDuration() > alertcontrol.getThresholdDuration();
            boolean isRatioThresholdExceeded = nrequests.getRatio() > alertcontrol.getThresholdRatio();
            boolean isNRequestsThresholdExceeded = nrequests.getNRequests() > alertcontrol.getThresholdRequests();
            if (isTotalDurationThresholdExceeded || isRatioThresholdExceeded || isNRequestsThresholdExceeded) {
                if (foundNrequests.containsKey(nrequests.getTitleId())) {
                    if (foundNrequests.get(nrequests.getTitleId()).getNRequests() < nrequests.getNRequests())
                        foundNrequests.replace(nrequests.getTitleId(), nrequests);
                } else {
                    foundNrequests.put(nrequests.getTitleId(), nrequests);
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
            String contentString = "<h2>" + nrequests.getShelfmark() + ":</h2>";
            contentString += nrequests.getMab() + "<br /> ";
            contentString += "<table>";
            contentString += "<tr><td>Anzahl Vormerkungen:</td><td>" + nrequests.getNRequests() + "</td></tr>";
            contentString += "<tr><td>Ausleihbare Exemplare:</td><td>" + nrequests.getNLendable() + "</td></tr>";
            contentString += "<tr><td>Verhältnis:</td><td>" + String.format("%.2f", nrequests.getRatio()) + "</td></tr>";
            contentString += "<tr><td>Dauer:</td><td>" + nrequests.geTotalDuration() + "</td></tr>";
            contentString += "</table> \n <br />";
            contentString += "<a href=\"https://lib-intel.ub.uni-due.de/protokoll?shelfmark=" + nrequests.getShelfmark() + "&exact=\">Zum Ausleihprotokoll</a>";

            content.setValue(contentString);
            entry.setContents(Collections.singletonList(content));
            entries.add(entry);
        }
        return entries;
    }

    private void updateNrequests(Collection<Nrequests> nrequestss) {
        int succesfullPosts = 0;
        for (Nrequests nrequest : nrequestss) {
            stockanalyzerClient.saveNrequests(nrequest);
            succesfullPosts++;
        }
        log.info("successfully posted " + succesfullPosts);
    }

}
