package vn.edu.iuh.nhom01_www.recommendation.model;

public enum PersonaType {
    DALAT_LIMOUSINE("Hội đam mê phượt Đà Lạt Limousine", "Khách hàng ưu tiên xe VIP Limousine, giờ xuất phát buổi tối (20h-23h) đi Đà Lạt"),
    NHATRANG_FAMILY("Hội du lịch gia đình & công tác Nha Trang", "Khách hàng chuộng xe giường nằm cao cấp, đi ban ngày, chú trọng an toàn tiện nghi"),
    BUDGET_SAVER("Hội sinh viên & du lịch tiết kiệm", "Khách hàng quan tâm mức giá tốt nhất, ưu tiên chuyến xe phổ thông tiết kiệm"),
    POPULAR_TRENDING("Khám phá tuyến xe thịnh hành", "Các chuyến xe hot nhất mùa lễ hội được đông đảo hành khách lựa chọn");

    private final String displayName;
    private final String description;

    PersonaType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

