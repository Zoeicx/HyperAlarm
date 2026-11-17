Chính sách này giải thích cách HyperAlarm (sau đây gọi là «mô-đun») xử lý thông tin của bạn. Kết luận trước tiên: mô-đun không thu thập và không tải lên bất kỳ dữ liệu cá nhân nào; nhật ký và cài đặt của bạn đều ở lại trên thiết bị này. Chi tiết được trình bày dưới đây.

## 0. Phiên bản và ngày có hiệu lực

- 0.1. Phiên bản chính sách: 1.0
- 0.2. Ngày có hiệu lực: 15 tháng 11 năm 2026
- 0.3. Chính sách này được phát hành cùng mỗi bản dựng.

## 1. Thông tin được lưu trên thiết bị

- 1.1. Nhật ký tập lệnh: thời gian, mã thoát, đầu ra chuẩn và lỗi chuẩn của mỗi lần thực thi tập lệnh. Được lưu trong cơ sở dữ liệu của mô-đun và thư mục lưu trữ ngoài của mô-đun.
- 1.2. Nhật ký ứng dụng: nhật ký hoạt động của chính mô-đun, dùng để chẩn đoán sự cố.
- 1.3. Bản ghi sự cố: các bản tóm tắt ngoại lệ ngắn được chế độ an toàn sử dụng, lưu trong thư mục riêng của mô-đun.
- 1.4. Cài đặt: ngôn ngữ, mức nhật ký, bộ lọc danh sách, v.v., được lưu trong tệp cấu hình của mô-đun.
- 1.5. Mật khẩu màn hình khóa: chỉ được ghi vào tệp khi bạn chủ động nhập trong phần cài đặt; tệp có quyền 600, chỉ root và mô-đun này có thể đọc; dùng để mở khóa màn hình trước khi thực thi tác vụ im lặng.
- 1.6. Không nội dung nào ở trên rời khỏi thiết bị của bạn, trừ khi chính bạn xuất, chia sẻ hoặc đăng lên mục báo lỗi.

## 2. Thông tin chúng tôi không thu thập

- 2.1. Không thu thập mã nhận dạng thiết bị (IMEI, Android ID, số sê-ri, v.v.).
- 2.2. Không thu thập dữ liệu vị trí.
- 2.3. Không thu thập danh bạ, tin nhắn hoặc lịch sử cuộc gọi.
- 2.4. Không thu thập nội dung tập lệnh của bạn.
- 2.5. Mô-đun không chứa SDK phân tích, báo cáo sự cố hoặc quảng cáo.

## 3. Hai điểm truy cập mạng

- 3.1. Trang «Giới thiệu» gửi yêu cầu đến các giao diện công khai của GitHub để đọc ảnh đại diện của tác giả và danh sách người đóng góp. Yêu cầu chỉ được gửi đến api.github.com và avatars.githubusercontent.com, mang User-Agent chuẩn do GitHub yêu cầu và không mang bất kỳ mã nhận dạng thiết bị hoặc người dùng nào. Do đó, GitHub sẽ thấy địa chỉ IP của bạn.
- 3.2. Trình hướng dẫn thiết lập lần đầu và trang cài đặt tải nội dung thỏa thuận người dùng và chính sách này từ GitHub để văn bản có thể được cập nhật độc lập với các bản cập nhật phiên bản. Các yêu cầu này cũng chỉ mang tiêu đề HTTP chuẩn. Nếu tải xuống thất bại, bản sao đi kèm trong gói cài đặt sẽ được hiển thị hoặc thông báo lỗi sẽ xuất hiện.
- 3.3. Ngoài các trường hợp trên, mô-đun không thực hiện bất kỳ yêu cầu mạng nào.

## 4. Chia sẻ với bên thứ ba

- 4.1. Chúng tôi không chia sẻ thông tin với bất kỳ bên thứ ba nào vì chúng tôi không thu thập thông tin.
- 4.2. Các yêu cầu GitHub nêu trên là tương tác trực tiếp của bạn với GitHub và chịu sự điều chỉnh của chính sách quyền riêng tư của GitHub.
- 4.3. Khi bạn chủ động xuất và chia sẻ nhật ký, người nhận do bạn lựa chọn; đây không phải là hành vi chia sẻ của chúng tôi.

## 5. Lưu giữ và xóa

- 5.1. Nhật ký được tự động cắt bớt theo giới hạn cố định, xóa các bản ghi cũ nhất trước.
- 5.2. Bạn có thể xóa nhật ký trong ứng dụng hoặc xóa trực tiếp dữ liệu của mô-đun.
- 5.3. Gỡ cài đặt mô-đun sẽ xóa toàn bộ dữ liệu trong các thư mục riêng của mô-đun.
- 5.4. Bản sao nhật ký trong bộ nhớ ngoài cần được bạn xóa thủ công trước khi gỡ cài đặt.

## 6. Quyền của bạn

- 6.1. Bạn có thể xem, xuất và xóa toàn bộ thông tin do mô-đun lưu trữ bất cứ lúc nào.
- 6.2. Bạn có thể từ chối bất kỳ quyền nào; chức năng tương ứng sẽ không khả dụng.
- 6.3. Bạn có thể xóa mật khẩu màn hình khóa trong phần cài đặt; nội dung tệp sẽ bị làm trống.

## 7. Người chưa thành niên

- 7.1. Mô-đun hướng đến người dùng có kiến thức nhất định về hệ thống. Người chưa thành niên nên sử dụng dưới sự hướng dẫn của người giám hộ.
- 7.2. Vì liên quan đến root và thực thi tập lệnh, chúng tôi không khuyến khích người chưa thành niên tự thao tác.

## 8. Thay đổi chính sách

- 8.1. Chính sách này có thể được sửa đổi khi phát hành phiên bản mới; nội dung sửa đổi được phát hành cùng bản dựng mới và công bố trong kho.
- 8.2. Nếu một bản sửa đổi liên quan đến cách xử lý thông tin mới, chúng tôi sẽ ghi rõ trong ghi chú cập nhật.

## 9. Liên hệ

- 9.1. Vui lòng phản hồi các vấn đề về quyền riêng tư qua mục báo lỗi trong kho dự án.
- 9.2. Mã nguồn của mô-đun được công khai và có thể kiểm toán; bạn có thể tự mình xác minh các tuyên bố trên.
