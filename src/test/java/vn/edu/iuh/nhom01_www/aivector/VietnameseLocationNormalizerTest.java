package vn.edu.iuh.nhom01_www.aivector;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VietnameseLocationNormalizerTest {

    private final VietnameseLocationNormalizer normalizer = new VietnameseLocationNormalizer();

    @Test
    void canonicalizesSaiGonAndHoChiMinhToTheStoredTpHcmIdentity() {
        assertThat(normalizer.normalize("Sài Gòn")).isEqualTo("tp hcm");
        assertThat(normalizer.normalize("Thành phố Hồ Chí Minh")).isEqualTo("tp hcm");
        assertThat(normalizer.normalize("TP.HCM")).isEqualTo("tp hcm");
    }

    @Test
    void extractsDirectionalVietnameseQueriesWithoutReversingEndpoints() {
        assertThat(normalizer.directionalLocations("Tôi muốn đi từ Sài Gòn đến Đà Lạt"))
                .contains(new VietnameseLocationNormalizer.DirectionalLocation("tp hcm", "da lat"));
        assertThat(normalizer.directionalLocations("Tìm chuyến từ TP.HCM đi Nha Trang"))
                .contains(new VietnameseLocationNormalizer.DirectionalLocation("tp hcm", "nha trang"));
    }

    @Test
    void doesNotPretendAMultiLegQueryIsOneDirectionalRoute() {
        assertThat(normalizer.directionalLocations("Từ TP.HCM đến Đà Lạt rồi đến Nha Trang"))
                .isEmpty();
        assertThat(normalizer.directionalLocations("Từ TP.HCM qua Đà Lạt đến Nha Trang"))
                .isEmpty();
    }
}
