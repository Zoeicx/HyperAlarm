<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | **Tiếng Việt** | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm là một mô-đun Xposed với **phạm vi duy nhất là Đồng hồ Xiaomi**. Nó biến báo thức của đồng hồ thành thứ kích hoạt các script shell.

## Ứng dụng đích

Chỉ được kiểm chứng trên các phiên bản dưới đây. Với bản khác, bạn tự thử, gửi APK để được thích ứng, hoặc cập nhật/hạ cấp về đúng các bản này:

- Đồng hồ Xiaomi `com.android.deskclock`
- Phiên bản `18.28.0`, mã phiên bản `1306002800`

## Cài đặt

1. Chuẩn bị root (KernelSU / Magisk) và LSPosed (Xposed API 101 trở lên).
2. Cài mô-đun rồi bật HyperAlarm trong LSPosed.
3. Phạm vi chỉ đánh dấu `com.android.deskclock`. Đừng thêm gói nào khác.
4. Buộc dừng Đồng hồ Xiaomi rồi để tiến trình khởi động lại, nếu không mô-đun sẽ không chạy.

## Cách dùng

- Tạo nhiệm vụ: nút `+` ở bên phải thanh thao tác phía trên.
- Nhãn báo thức chính là tên script và phải kết thúc bằng `.sh`:
  - `alert.sh`: chạy lặng lẽ khi tới giờ, không đổ chuông, không hiện màn hình báo thức
  - `@alert.sh`: chạy script và vẫn giữ màn hình báo thức cùng nhạc chuông
- Dấu `@` được ẩn trong danh sách; gạt công tắc «Nhạc chuông» để chuyển giữa hai chế độ.
- «Chạy ngay» cho phép thử script mà không phải chờ tới giờ.
- Nhiệm vụ theo ca kích hoạt theo từng ca, tính từ ngày bắt đầu và chu kỳ.

## Quy tắc cho script

Script nằm trong `/data/adb/alarm_shell/scripts/`, tên trong nhãn phải trùng với tên tệp.

```text
/data/adb/alarm_shell/scripts/      # thư mục script
/data/adb/alarm_shell/password      # mật khẩu màn hình khóa
/data/adb/alarm_shell/unlock_phone  # script mở khóa mặc định
```

- Xuống dòng phải là LF. Tệp có CRLF từ Windows sẽ hỏng ngay.
- Script chạy bằng quyền root trong không gian tên chính của hệ thống nên thấy được thư mục dữ liệu của ứng dụng khác.
- Mỗi lần chạy đều để lại log: xem ở trang «Log script» trong ứng dụng, đồng thời được sao sang
  `/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<tên script>/`.
- Trước nhiệm vụ lặng lẽ, nếu bạn đã đặt mật khẩu màn hình khóa, mô-đun sẽ dùng nó để mở khóa.

## Cài đặt trong ứng dụng

- **Đồng hồ chỉ hiện báo thức**: danh sách Đồng hồ chỉ còn báo thức thật, nhiệm vụ script bị ẩn.
- **Ẩn nhiệm vụ có chuông**: ẩn luôn cả nhiệm vụ đang bật nhạc chuông.
- Mức log / Log ứng dụng / Log script: khi cần tìm lỗi hãy nâng mức log trước.
- **Mật khẩu màn hình khóa**: dùng để mở khóa trước nhiệm vụ lặng lẽ; để trống nghĩa là không đổi.
- Hướng dẫn khởi động lại, sao chép thư mục script, sao lưu và phục hồi cấu hình.

## Hạn chế đã biết

- Chỉ kiểm chứng trên Android 16 / HyperOS 3 / Đồng hồ 18.28.0; tổ hợp khác bạn tự chịu trách nhiệm.
- Chế độ theo ca phụ thuộc vào phần bên trong của Đồng hồ, sau khi cập nhật đồng hồ phải kiểm chứng lại.
- Không có root thì mô-đun không chạy; trang cài đặt sẽ báo.
- Log script được sao sang bộ nhớ ngoài, root và adb shell đều đọc được. Đừng in khóa bí mật ra log.

## Câu hỏi thường gặp

- **Đã thêm nhiệm vụ mà đồng hồ không phản ứng**: kiểm tra mô-đun đã bật trong LSPosed, phạm vi chỉ là Đồng hồ Xiaomi, và tiến trình đồng hồ đã được khởi động lại.
- **Nhiệm vụ không hiện trong danh sách đồng hồ**: «Đồng hồ chỉ hiện báo thức» đang làm việc, tắt nó đi.
- **Tới giờ mà không chạy**: xem log script trong ứng dụng; thiếu script hay nhãn gõ sai đều được báo rõ ở đó.

## Giấy phép

- Mã của kho này: **AGPL-3.0** (xem `../LICENSE`)
- Thành phần bên thứ ba và giấy phép: [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## Cảm ơn

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler): điểm khởi đầu của dự án; phần lớn giao diện MIUIX và khung cài đặt đến từ đó
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix): thành phần giao diện kiểu Xiaomi
- [libsu](https://github.com/topjohnwu/libsu), [libxposed](https://github.com/libxposed/api): API cho root và Xposed
- Tất cả những ai đã mở issue và gửi log
