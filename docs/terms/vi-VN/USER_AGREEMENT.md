HyperAlarm là một mô-đun tăng cường hệ thống phi thương mại, chạy trên nền tảng Xposed và hướng đến Mi Clock. Nó biến báo thức của Mi Clock thành cơ chế kích hoạt tập lệnh root: khi báo thức đến giờ, tập lệnh do bạn cung cấp có thể được thực thi thay vì đổ chuông, hoặc cùng với tiếng chuông. Vui lòng đọc và hiểu Thỏa thuận người dùng HyperAlarm này trước khi tiếp tục sử dụng.

## 0. Phiên bản và ngày có hiệu lực

- 0.1. Phiên bản thỏa thuận: 1.0
- 0.2. Ngày có hiệu lực: 15 tháng 11 năm 2026
- 0.3. Thỏa thuận này được phát hành cùng mỗi bản dựng; phiên bản mới nhất có trong kho dự án.

## 1. Quy định chung

- 1.1. HyperAlarm do Nhóm Dự án HyperAlarm (sau đây gọi là «chúng tôi») phát triển và duy trì.
- 1.2. Mô-đun yêu cầu thiết bị đã có quyền root và đã cài đặt nền tảng Xposed / LSPosed; nếu thiếu một trong hai điều kiện, mô-đun sẽ không hoạt động.
- 1.3. Phạm vi duy nhất của mô-đun là Mi Clock (com.android.deskclock). Chúng tôi không đưa hook vào bất kỳ ứng dụng nào khác.
- 1.4. Khi sử dụng mô-đun, bạn xác nhận rằng mình đã hiểu: mô-đun khiến báo thức thực thi tập lệnh do bạn cung cấp vào thời điểm đã đặt, thay vì chỉ đổ chuông.
- 1.5. Bạn có thể ngừng sử dụng bất cứ lúc nào bằng cách gỡ cài đặt mô-đun hoặc vô hiệu hóa mô-đun trong trình quản lý Xposed.

## 2. Phạm vi giấy phép

- 2.1. Chúng tôi cấp cho bạn giấy phép cá nhân, không thể chuyển nhượng, không được cấp phép lại và không độc quyền để cài đặt và sử dụng mô-đun.
- 2.2. Mã nguồn được phát hành theo GNU Affero General Public License v3.0 (AGPL-3.0). Bạn có thể sao chép, sửa đổi và phân phối lại trong phạm vi giấy phép đó cho phép.
- 2.3. Việc cài đặt sẵn mang tính thương mại, bán kèm hoặc phân phối cùng thiết bị cần có sự cho phép trước bằng văn bản của chúng tôi.
- 2.4. Các phần kế thừa từ HyperCeiler vẫn tuân theo giấy phép ban đầu của HyperCeiler; xem THIRD_PARTY_NOTICES.md trong kho.
- 2.5. Tên và biểu tượng HyperAlarm không thuộc phạm vi giấy phép mã nguồn.

## 3. Khả năng và trách nhiệm của bạn

- 3.1. Tập lệnh được thực thi với quyền root trong không gian tên mount chính của hệ thống, nên có thể thấy thư mục dữ liệu của các ứng dụng khác.
- 3.2. Bạn tự cung cấp, cài đặt và duy trì tập lệnh. Chúng tôi không chịu trách nhiệm về hành vi của tập lệnh hoặc bất kỳ hậu quả nào do việc thực thi tập lệnh gây ra.
- 3.3. Tác vụ im lặng sẽ ức chế giao diện báo thức; nếu tập lệnh bị thiếu, bị từ chối hoặc không thể lên lịch lại báo thức, mô-đun sẽ giữ lại tiếng chuông báo thức bình thường.
- 3.4. Mô-đun này không phải là phương tiện nhắc nhở đáng tin cậy. Không sử dụng cho các tình huống mà việc bỏ lỡ báo thức có thể gây thương tích cho người hoặc tổn thất tài sản nghiêm trọng, bao gồm nhưng không giới hạn ở y tế, phòng cháy chữa cháy hoặc trực bảo vệ.
- 3.5. Việc sửa đổi hành vi hệ thống tiềm ẩn rủi ro, bao gồm nhưng không giới hạn ở việc thiết bị không khởi động được, mất dữ liệu hoặc mất hiệu lực bảo hành.

## 4. Xử lý thông tin

- 4.1. Nhật ký tập lệnh, nhật ký ứng dụng, bản ghi sự cố, cài đặt và mật khẩu màn hình khóa đều chỉ được lưu trên thiết bị của bạn; chúng tôi không tải lên, không thu thập và không bán chúng.
- 4.2. Mô-đun truy cập mạng ở đúng hai vị trí: trang «Giới thiệu» lấy ảnh đại diện của tác giả và danh sách người đóng góp từ các giao diện công khai của GitHub; nội dung thỏa thuận được tải từ GitHub. Cả hai chỉ để lộ địa chỉ IP của bạn cho GitHub và không mang theo bất kỳ mã nhận dạng thiết bị nào.
- 4.3. Chi tiết được nêu trong Chính sách quyền riêng tư HyperAlarm phát hành cùng thỏa thuận này.

## 5. Quyền cần bạn cấp

- 5.1. Root: thực thi tập lệnh, đọc và ghi thư mục tập lệnh, đồng bộ nhật ký, sửa đổi cài đặt toàn hệ thống. Đây là yêu cầu bắt buộc đối với các chức năng cốt lõi.
- 5.2. Danh sách ứng dụng đã cài đặt: để hiển thị ứng dụng trong trình chọn phạm vi.
- 5.3. Thông báo: hiển thị quá trình thực thi và kết quả của tập lệnh, cũng như lý do thất bại.
- 5.4. Mạng: chỉ trong hai trường hợp được mô tả tại mục 4.2.
- 5.5. Bạn có thể từ chối bất kỳ quyền nào; chức năng tương ứng sẽ không khả dụng, nhưng các chức năng còn lại không bị ảnh hưởng.

## 6. Quy tắc sử dụng

- 6.1. Bạn không được xóa thông tin về quyền tác giả và giấy phép khỏi mô-đun hoặc các bản sao của mô-đun.
- 6.2. Bạn không được sử dụng mô-đun để gây tổn hại an ninh mạng, xâm phạm quyền riêng tư của người khác hoặc thực hiện hành vi vi phạm pháp luật khác.
- 6.3. Bạn không được cài đặt hoặc sử dụng mô-đun trên thiết bị của người khác mà không có sự đồng ý của chủ sở hữu thiết bị.
- 6.4. Bạn không được sử dụng mô-đun để vượt qua thanh toán, bẻ khóa phần mềm hoặc né tránh kiểm soát truy cập.

## 7. Miễn trừ và giới hạn trách nhiệm

- 7.1. Mô-đun được cung cấp «nguyên trạng», không kèm bất kỳ bảo đảm nào, dù rõ ràng hay ngụ ý, bao gồm nhưng không giới hạn ở bảo đảm về khả năng bán được, khả năng phù hợp cho một mục đích cụ thể và không xâm phạm quyền của bên thứ ba.
- 7.2. Chúng tôi chỉ xác minh chức năng trên Mi Clock 18.28.0. Sau khi đồng hồ hoặc hệ thống được cập nhật, một số chức năng có thể ngừng hoạt động hoặc thay đổi hành vi.
- 7.3. Các vấn đề do hành động của bạn, nội dung tập lệnh, môi trường root, nền tảng Xposed hoặc bản cập nhật hệ thống gây ra sẽ do bạn tự chịu trách nhiệm.
- 7.4. Trong phạm vi pháp luật cho phép tối đa, chúng tôi không chịu trách nhiệm về thiệt hại gián tiếp, ngẫu nhiên, đặc biệt hoặc mang tính trừng phạt phát sinh từ việc sử dụng hoặc không thể sử dụng mô-đun này.
- 7.5. Nếu một điều khoản bị tuyên bố vô hiệu, các điều khoản còn lại vẫn có hiệu lực.

## 8. Thay đổi và chấm dứt

- 8.1. Chúng tôi có thể sửa đổi thỏa thuận này khi phát hành phiên bản mới; nội dung sửa đổi được phát hành cùng bản dựng mới và công bố trong kho.
- 8.2. Việc bạn tiếp tục sử dụng sau khi bản sửa đổi có hiệu lực được coi là chấp nhận nội dung sửa đổi; nếu không chấp nhận, vui lòng ngừng sử dụng và gỡ cài đặt.
- 8.3. Bạn có thể chấm dứt thỏa thuận này bất cứ lúc nào bằng cách gỡ cài đặt mô-đun.

## 9. Luật áp dụng và giải quyết tranh chấp

- 9.1. Thỏa thuận này được điều chỉnh bởi pháp luật nước Cộng hòa Nhân dân Trung Hoa.
- 9.2. Các tranh chấp phát sinh từ thỏa thuận này trước hết phải được giải quyết thông qua thương lượng thiện chí; nếu không đạt được thỏa thuận, sẽ được đưa ra tòa án nhân dân có thẩm quyền tại trụ sở kinh doanh chính của chúng tôi.

## 10. Liên hệ

- 10.1. Vui lòng phản hồi các vấn đề liên quan đến thỏa thuận này qua mục báo lỗi trong kho dự án.
- 10.2. Nếu bạn cho rằng một điều khoản nào đó không khớp với hành vi thực tế, vui lòng thông báo cho chúng tôi để sửa lại, thay vì coi điều khoản đó là ràng buộc.
