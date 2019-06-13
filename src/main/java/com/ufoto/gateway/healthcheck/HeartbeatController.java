package com.ufoto.gateway.healthcheck;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 心跳反馈
 *
 * @author Luo Bao Ding
 * @since 2018/5/18
 */
@RestController
public class HeartbeatController {

    @RequestMapping("/heartbeat")
    public String heartbeat() {
        return "success";
    }
}
