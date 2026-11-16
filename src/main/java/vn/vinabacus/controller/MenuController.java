package vn.vinabacus.controller;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MucMenu;
import vn.vinabacus.service.MenuService;

public final class MenuController {
    private final MenuService service;
    private CoSo coSo = CoSo.TRUNG_TAM;

    public MenuController(MenuService service) {
        this.service = Objects.requireNonNull(service);
    }
    public CoSo coSoHienTai() { return coSo; }
    public void chonCoSo(CoSo moi) { coSo = Objects.requireNonNull(moi); }
    public List<MucMenu> layMenu() { return service.layMenu(coSo); }
    public MucMenu chonMenu(String ma) {
        return layMenu().stream()
                .filter(menu -> menu.ma().equals(ma))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mục này không thuộc cơ sở đang chọn: " + ma));
    }
}
