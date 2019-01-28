package unidue.ub.rssfeeder.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import unidue.ub.rssfeeder.model.Alertcontrol;
import unidue.ub.rssfeeder.model.Nrequests;

import java.util.List;


@FeignClient(name="stock-analyzer", configuration = FeignConfiguration.class)
@Component
public interface StockanalyzerClient {

    @RequestMapping(method= RequestMethod.GET, value="/alertcontrol/{identifier}")
    Resource<Alertcontrol> getAlertcontrol(@PathVariable("identifier") String identifier);

    @RequestMapping(method = RequestMethod.GET, value="/nrequests/getForTimeperiod")
    List<Nrequests> getNrequestsForTimeperiod(@RequestParam("startNotation") String startNotation, @RequestParam("endNotation") String endNotation, @RequestParam("timeperiod") Long timeperiod);

    @RequestMapping(method = RequestMethod.POST, value="/nrequests")
    Nrequests saveNrequests(Nrequests nrequests);

}
