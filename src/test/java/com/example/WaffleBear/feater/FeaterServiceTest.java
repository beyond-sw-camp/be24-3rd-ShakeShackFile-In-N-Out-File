package com.example.WaffleBear.feater;

import com.example.WaffleBear.feater.model.Feater;
import com.example.WaffleBear.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeaterServiceTest {

    @Mock
    private FeaterRepository featerRepository;

    @Test
    void resolveProfileImages_allowsNullProfileImageValues() {
        FeaterService service = new FeaterService(featerRepository, null, null, null, null);

        when(featerRepository.findAllByUser_IdxIn(List.of(1L, 2L)))
                .thenReturn(List.of(
                        feater(1L, null),
                        feater(2L, "https://example.com/profile-2.png")
                ));

        Map<Long, String> result = service.resolveProfileImages(Arrays.asList(1L, null, 2L, 1L));

        assertThat(result).containsEntry(2L, "https://example.com/profile-2.png");
        assertThat(result).containsKey(1L);
        assertThat(result.get(1L)).isNull();
    }

    private Feater feater(Long userIdx, String profileImageUrl) {
        return Feater.builder()
                .user(User.builder().idx(userIdx).build())
                .profileImageUrl(profileImageUrl)
                .build();
    }
}
