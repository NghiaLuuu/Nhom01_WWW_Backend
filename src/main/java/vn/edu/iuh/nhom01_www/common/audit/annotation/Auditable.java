package vn.edu.iuh.nhom01_www.common.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu trên method Controller/Service cần ghi Audit Log.
 * Dùng cho các hành vi KHÔNG thay đổi dữ liệu trong DB
 * (ví dụ: chatbot, login, search, view), vì Hibernate Listener
 * chỉ bắt được INSERT/UPDATE/DELETE.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    /** Tên hành động (ví dụ: CHATBOT_ASK, LOGIN, SEARCH) */
    String action();

    /** Tên entity/module liên quan (ví dụ: Chatbot, Auth, Trip) */
    String entityName() default "";
}
