package com.aptech.aptechMall.dto.address;

import com.aptech.aptechMall.model.enums.AddressType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an address
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressRequest {

    @Size(max = 191, message = "Tên người nhận không được quá 191 ký tự")
    private String receiverName;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String phone;

    @Size(max = 100, message = "Tỉnh/Thành phố không được quá 100 ký tự")
    private String province;

    @Size(max = 100, message = "Quận/Huyện không được quá 100 ký tự")
    private String district;

    @Size(max = 100, message = "Phường/Xã không được quá 100 ký tự")
    private String ward;

    @Size(max = 500, message = "Địa chỉ chi tiết không được quá 500 ký tự")
    private String addressDetail;

    private AddressType addressType;
}
