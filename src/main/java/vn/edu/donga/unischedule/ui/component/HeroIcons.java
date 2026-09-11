package vn.edu.donga.unischedule.ui.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.Icon;
import java.awt.Color;
import java.text.Normalizer;
import java.util.Locale;

public final class HeroIcons {
    private HeroIcons() { }

    public static Icon of(String name, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon("icons/" + name + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        return icon;
    }

    public static String action(String text) {
        String value = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace('đ', 'd');
        if (value.contains("danh dau")) return "check-circle";
        if (value.contains("phan cong")) return "users";
        if (value.contains("bao hong") || value.contains("xung dot")) return "exclamation-triangle";
        if (value.contains("bao tri") || value.contains("khoa") || value.contains("mat khau")) return "cog-6-tooth";
        if (value.contains("cap nhat")) return "pencil-square";
        if (value.contains("yeu cau")) return "document-text";
        if (value.contains("xoa")) return "trash";
        if (value.contains("sua")) return "pencil-square";
        if (value.contains("them") || value.contains("tao")) return "plus";
        if (value.contains("luu") || value.contains("duyet")) return "check";
        if (value.contains("huy") || value.contains("tu choi") || value.contains("dong")) return "x-mark";
        if (value.contains("tim") || value.contains("tra cuu")) return "magnifying-glass";
        if (value.contains("truoc")) return "chevron-left";
        if (value.contains("sau")) return "chevron-right";
        if (value.contains("xuat")) return "arrow-down-tray";
        if (value.contains("dat lai") || value.contains("lam moi")) return "arrow-path";
        if (value.equals("an")) return "eye-slash";
        if (value.contains("tuan") || value.contains("lich")) return "calendar-days";
        if (value.contains("xem") || value.contains("hien")) return "eye";
        if (value.contains("dang nhap")) return "arrow-right-on-rectangle";
        return "chevron-right";
    }
}
