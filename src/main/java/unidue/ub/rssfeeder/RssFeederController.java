package unidue.ub.rssfeeder;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import unidue.ub.rssfeeder.clients.StockanalyzerClient;
import unidue.ub.rssfeeder.model.Nrequests;

import java.util.*;

@Controller
public class RssFeederController {

    private final StockanalyzerClient stockanalyzerClient;

    private Hashtable<String, Nrequests> foundNrequests;

    @Autowired
    public RssFeederController(StockanalyzerClient stockanalyzerClient) {
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

        List<Nrequests> nrequestss = stockanalyzerClient.getForAlertcontrol(identifier, requestor.orElse(""));
        for (Nrequests nrequests: nrequestss) {
            if (foundNrequests.containsKey(nrequests.getTitleId())) {
                if (foundNrequests.get(nrequests.getTitleId()).getNRequests() < nrequests.getNRequests())
                    foundNrequests.replace(nrequests.getTitleId(), nrequests);
            } else {
                foundNrequests.put(nrequests.getTitleId(), nrequests);
            }
        }
        feed.setEntries(getFeedListFromNrequests());
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

}
