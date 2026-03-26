package com.example.WaffleBear.legup.openhexagon;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.legup.LegupGameAccessService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/openhexagon")
@Tag(name = "Game", description = "Legup mini game APIs")
@SecurityRequirement(name = "bearerAuth")
public class OpenHexagonController {

    private final LegupGameAccessService legupGameAccessService;
    private final OpenHexagonService openHexagonService;

    @GetMapping("/state")
    @Operation(summary = "Get Open Hexagon state", description = "Returns the current Open Hexagon game snapshot.")
    public BaseResponse<?> state(@AuthenticationPrincipal AuthUserDetails user) {
        if (user == null || user.getIdx() == null) {
            throw new RuntimeException("Authentication is required.");
        }
        legupGameAccessService.ensurePlayableForHttp(user);

        return BaseResponse.success(openHexagonService.getSnapshot());
    }
}
