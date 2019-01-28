package unidue.ub.rssfeeder.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import unidue.ub.rssfeeder.model.Notationgroup;

@FeignClient(name="settings-backend", configuration = FeignConfiguration.class)
@Component
public interface SettingsClient {

    @RequestMapping(method= RequestMethod.GET, value="/notationgroup/{identifier}")
    Resource<Notationgroup> getNotationgroup(@PathVariable("identifier") String identifier);

}
