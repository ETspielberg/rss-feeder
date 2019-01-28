package unidue.ub.rssfeeder.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@FeignClient(name="blacklist-backend", configuration = FeignConfiguration.class)
@Component
public interface BlacklistClient {

    @RequestMapping(method= RequestMethod.GET, value="/getIgnoredFor/{identifier}")
    Boolean isBlocked(@PathVariable("identifier") String identifier, String type);

}
