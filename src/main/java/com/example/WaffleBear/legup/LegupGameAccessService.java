package com.example.WaffleBear.legup;

import com.example.WaffleBear.file.service.StoragePlanService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LegupGameAccessService {

    private final StoragePlanService storagePlanService;

    public void ensurePlayable(AuthUserDetails user) {
        if (!canPlay(user)) {
            throw new IllegalStateException("프리미엄 이상 계정만 게임을 플레이할 수 있습니다.");
        }
    }

    public void ensurePlayableForHttp(AuthUserDetails user) {
        if (!canPlay(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "프리미엄 이상 계정만 게임을 플레이할 수 있습니다.");
        }
    }

    public boolean canPlay(AuthUserDetails user) {
        if (user == null || user.getIdx() == null) {
            return false;
        }

        if (storagePlanService.isAdministrator(user.getIdx())) {
            return true;
        }

        String planCode = storagePlanService.resolveQuota(user.getIdx()).planCode();
        return "PREMIUM".equalsIgnoreCase(planCode) || "ADMIN".equalsIgnoreCase(planCode);
    }
}
