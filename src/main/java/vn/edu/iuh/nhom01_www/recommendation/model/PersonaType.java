package vn.edu.iuh.nhom01_www.recommendation.model;

public enum PersonaType {
    COMMUTER_HABIT("Tuyến cố định & Khứ hồi", "Khách hàng thường xuyên đi lại giữa 2 địa phương quen thuộc (đi làm, về quê, công tác)"),
    DALAT_LIMOUSINE("Nghỉ dưỡng Đà Lạt Limousine VIP", "Khách hàng ưu tiên xe VIP Limousine, giờ xuất phát buổi tối (20h-23h) đi Đà Lạt"),
    NHATRANG_FAMILY("Du lịch gia đình & Công tác Nha Trang", "Khách hàng chuộng xe giường nằm cao cấp, đi ban ngày, chú trọng an toàn tiện nghi"),
    BUDGET_SAVER("Tối ưu & Tiết kiệm chi phí", "Khách hàng quan tâm mức giá tốt nhất, ưu tiên chuyến xe phổ thông tiết kiệm"),
    EXPLORER_MULTI_DESTINATION("Khách thân thiết đi nhiều tỉnh thành", "Khách hàng VIP có lịch sử di chuyển phong phú qua nhiều tỉnh thành khắp cả nước"),
    POPULAR_TRENDING("Tuyến xe thịnh hành", "Các chuyến xe hot nhất mùa lễ hội được đông đảo hành khách lựa chọn");

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
