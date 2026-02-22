package com.smartLive.interaction.controller.inner;

import com.smartLive.interaction.service.ISyncDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal sync trigger endpoint for Feign/job calls.
 */
@RestController
@RequestMapping("/inner/sync")
@Slf4j
public class SyncInnerController
{
    @Autowired
    private ISyncDataService syncDataService;

    @PostMapping("/trigger")
    public Boolean trigger()
    {
        try
        {
            syncDataService.syncAllData();
            return true;
        }
        catch (Exception e)
        {
            log.error("trigger sync failed", e);
            return false;
        }
    }
}
