package com.example.WaffleBear.legup.openhexagon;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.legup.LegupGameAccessService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/openhexagon")
public class OpenHexagonController {

    private final LegupGameAccessService legupGameAccessService;
    private final OpenHexagonService openHexagonService;

    @GetMapping("/state")
    public BaseResponse<?> state(@AuthenticationPrincipal AuthUserDetails user) {
        if (user == null || user.getIdx() == null) {
            throw new RuntimeException("Authentication is required.");
        }
        legupGameAccessService.ensurePlayableForHttp(user);

        return BaseResponse.success(openHexagonService.getSnapshot());
    }
}
