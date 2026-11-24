
package vn.vinabacus;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.service.CoSoService;

public class KiemTraCoSo {

    public static void main(String[] args) {

        CoSoService service = new CoSoService();

        try {
            int maTrungTam =
                service.layMaCoSo(CoSo.TRUNG_TAM);

            int maMamNon =
                service.layMaCoSo(CoSo.MAM_NON);

            System.out.println("TRUNG TAM: " + maTrungTam);
            System.out.println("MAM NON: " + maMamNon);

            System.out.println("KIEM TRA THANH CONG!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
