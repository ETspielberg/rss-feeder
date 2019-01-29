package unidue.ub.rssfeeder.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import unidue.ub.rssfeeder.model.Nrequests;

import java.util.List;


@FeignClient(name="stock-analyzer", configuration = FeignConfiguration.class)
@Component
public interface StockanalyzerClient {

    @RequestMapping(method = RequestMethod.GET, value="/nrequests/forAlertcontrol/{alertcontrol}")
    List<Nrequests> getForAlertcontrol(@PathVariable("identifier") String identifier, @RequestParam("requestor") String requestor);


}
