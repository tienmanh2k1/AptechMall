import React from 'react';

const Footer = () => {
  return (
    <footer className="bg-gray-900 text-gray-300 mt-16">
      <div className="container mx-auto px-4 py-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div>
            <h3 className="text-white font-semibold mb-4">Về AptechMall</h3>
            <p className="text-sm">
              Nền tảng mua sắm trực tuyến uy tín, kết nối sản phẩm chất lượng từ Trung Quốc đến Việt Nam.
            </p>
          </div>

          <div>
            <h3 className="text-white font-semibold mb-4">Dịch vụ khách hàng</h3>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-white">Trung tâm trợ giúp</a></li>
              <li><a href="#" className="hover:text-white">Theo dõi đơn hàng</a></li>
              <li><a href="#" className="hover:text-white">Đổi trả hàng</a></li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-semibold mb-4">Liên kết nhanh</h3>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-white">Tài khoản của tôi</a></li>
              <li><a href="#" className="hover:text-white">Đơn hàng</a></li>
              <li><a href="#" className="hover:text-white">Danh sách yêu thích</a></li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-semibold mb-4">Liên hệ</h3>
            <ul className="space-y-2 text-sm">
              <li>Email: support@aptechmall.com</li>
              <li>Điện thoại: +84 123 456 789</li>
              <li>Giờ làm việc: 9h - 18h (GMT+7)</li>
            </ul>
          </div>
        </div>

        <div className="border-t border-gray-800 mt-8 pt-8 text-center text-sm">
          <p>&copy; 2025 AptechMall. Tất cả quyền được bảo lưu.</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;