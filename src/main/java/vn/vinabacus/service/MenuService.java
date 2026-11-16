package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.config.MenuConfig;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MucMenu;

public final class MenuService {
    public List<MucMenu> layMenu(CoSo coSo) {
        return MenuConfig.choCoSo(Objects.requireNonNull(coSo));
    }
}
