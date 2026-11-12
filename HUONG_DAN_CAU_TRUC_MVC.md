# VINABACUS — cấu trúc MVC phẳng theo yêu cầu

- Package gốc: `vn.vinabacus`.
- Các lớp được chuyển về 7 package cấp một: `view`, `controller`, `model`, `service`, `data`, `config`, `repository`.
- Nội dung nghiệp vụ và file SQL **không được thay đổi** trong lần tái cấu trúc này.
- Phải kiểm thử kết nối SQL Server thực tế riêng; không được coi đây là bản hoàn thiện của 18 phân hệ.
- Từ thư mục chứa `pom.xml`, chạy `mvn clean test` (máy cần Maven và JDK 17+).
- Chạy giao diện với `mvn exec:java -Dexec.mainClass=vn.vinabacus.UngDungVINABACUS` nếu đã cài plugin exec phù hợp, hoặc chạy class `UngDungVINABACUS` từ NetBeans.

## Nghiệp vụ

Bảng 18 chức năng và các nhận xét đối chiếu SQL do người dùng cung cấp là danh sách yêu cầu/phần việc chưa hoàn tất; **không được coi là 18 chức năng đã được triển khai**.

## Lưu ý

- Khi áp dụng vào dự án có sửa đổi cục bộ, hãy sao lưu và đối chiếu Git trước khi thay thế.
- Không chạy script tạo mới SQL trên dữ liệu đang sử dụng khi chưa có bản sao lưu và kế hoạch di trú.
