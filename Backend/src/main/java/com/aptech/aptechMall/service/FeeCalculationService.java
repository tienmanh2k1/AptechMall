package com.aptech.aptechMall.service;

import com.aptech.aptechMall.entity.Marketplace;
import com.aptech.aptechMall.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service tính phí đơn hàng theo cấu trúc phí của AptechMall
 *
 * Chức năng chính:
 * - Tính phí dịch vụ (Service Fee): 1.5% trên giá trị sản phẩm
 * - Tính phí kiểm đếm hàng (Item Count Check): Theo số lượng items
 * - Tính phí đóng gỗ (Wooden Packaging): Theo trọng lượng
 * - Tính phí bọc bong bóng (Bubble Wrap): Theo trọng lượng
 *
 * HỆ THỐNG PHÍ:
 *
 * 1. **Service Fee** (Phí dịch vụ):
 *    - 1.5% trên tổng giá trị sản phẩm
 *    - Tự động tính khi checkout
 *    - Đã bao gồm trong deposit 70%
 *
 * 2. **Item Count Check Fee** (Phí kiểm đếm hàng):
 *    - Đảm bảo số lượng hàng đúng như order
 *    - Phí theo số lượng items và loại hàng (regular/accessory)
 *    - Accessory: Sản phẩm giá < 10 CNY (phụ kiện nhỏ)
 *
 *    Regular Items (Hàng thường):
 *    - 1-5 items: 5,000đ/item
 *    - 6-20 items: 3,000đ/item
 *    - 21-100 items: 2,000đ/item
 *    - 101-500 items: 1,500đ/item
 *    - 501-10,000 items: 1,000đ/item
 *
 *    Accessory Items (Phụ kiện < 10 CNY):
 *    - 1-5 items: 2,500đ/item
 *    - 6-20 items: 2,000đ/item
 *    - 21-100 items: 1,500đ/item
 *    - 101-500 items: 1,000đ/item
 *    - 501-10,000 items: 800đ/item
 *
 * 3. **Wooden Packaging Fee** (Phí đóng gỗ):
 *    - Đóng gỗ để bảo vệ hàng dễ vỡ
 *    - Kg đầu tiên: 20 CNY
 *    - Mỗi kg thêm: 1 CNY/kg
 *    - Tự động convert CNY → VND
 *
 * 4. **Bubble Wrap Fee** (Phí bọc bong bóng):
 *    - Bọc bong bóng để chống va đập
 *    - Kg đầu tiên: 10 CNY
 *    - Mỗi kg thêm: 1.5 CNY/kg
 *    - Tự động convert CNY → VND
 *
 * LUỒNG TÍNH PHÍ TRONG ORDER:
 *
 * **Khi Checkout:**
 * - Product Cost: Tổng giá trị sản phẩm (convert sang VND)
 * - Service Fee: 1.5% của Product Cost (tính tự động)
 * - Total Cost = Product Cost + Service Fee
 * - Deposit = 70% của Total Cost
 * - Remaining = 30% của Total Cost
 *
 * **Sau khi Admin order hàng từ marketplace:**
 * - Admin cập nhật: Domestic shipping, International shipping, Weight
 * - Admin chọn additional services: Wooden packaging, Bubble wrap, Item count check
 * - System tính additional services fee (service này)
 * - Recalculate:
 *   - Total Amount = Product Cost + Service Fee + Domestic Shipping + International Shipping + Additional Services Fee
 *   - Remaining Amount = Total Amount - Deposit (đã trả 70%)
 *
 * CONVERT CURRENCY:
 * - Wooden Packaging Fee: CNY → VND (qua ExchangeRateService)
 * - Bubble Wrap Fee: CNY → VND (qua ExchangeRateService)
 * - Item Count Check: Đã tính bằng VND
 *
 * ACCESSORY DETECTION:
 * - Kiểm tra giá của từng OrderItem
 * - Nếu marketplace = AliExpress (USD) → convert USD → CNY
 * - Nếu marketplace = 1688 (CNY) → dùng trực tiếp
 * - Nếu price < 10 CNY → classify là accessory
 * - Accessory có phí kiểm đếm rẻ hơn (vì nhỏ, dễ đếm)
 *
 * TIER-BASED PRICING:
 * - Càng nhiều items → phí per item càng rẻ (bulk discount)
 * - Ví dụ: 500 items = 500 x 1,000đ = 500,000đ
 * - So với: 5 items = 5 x 5,000đ = 25,000đ (phí cao hơn per item)
 *
 * ROUNDING:
 * - Service Fee: Làm tròn HALF_UP về số nguyên
 * - Currency conversion: Làm tròn theo ExchangeRateService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeeCalculationService {

    private final ExchangeRateService exchangeRateService;

    // Service fee is 1.5% of product cost
    private static final BigDecimal SERVICE_FEE_PERCENTAGE = new BigDecimal("1.5");

    // Item count check fee (VND per item)
    private static final BigDecimal ITEM_COUNT_FEE_1_5 = new BigDecimal("5000");
    private static final BigDecimal ITEM_COUNT_FEE_6_20 = new BigDecimal("3000");
    private static final BigDecimal ITEM_COUNT_FEE_21_100 = new BigDecimal("2000");
    private static final BigDecimal ITEM_COUNT_FEE_101_500 = new BigDecimal("1500");
    private static final BigDecimal ITEM_COUNT_FEE_501_10000 = new BigDecimal("1000");

    // Accessory item (price < 10 CNY) count check fee
    private static final BigDecimal ACCESSORY_FEE_1_5 = new BigDecimal("2500");
    private static final BigDecimal ACCESSORY_FEE_6_20 = new BigDecimal("2000");
    private static final BigDecimal ACCESSORY_FEE_21_100 = new BigDecimal("1500");
    private static final BigDecimal ACCESSORY_FEE_101_500 = new BigDecimal("1000");
    private static final BigDecimal ACCESSORY_FEE_501_10000 = new BigDecimal("800");

    // Accessory threshold (CNY)
    private static final BigDecimal ACCESSORY_PRICE_THRESHOLD = new BigDecimal("10");

    // Wooden packaging fee (CNY)
    private static final BigDecimal WOODEN_PACKAGING_FIRST_KG = new BigDecimal("20");
    private static final BigDecimal WOODEN_PACKAGING_ADDITIONAL_KG = new BigDecimal("1");

    // Bubble wrap packaging fee (CNY)
    private static final BigDecimal BUBBLE_WRAP_FIRST_KG = new BigDecimal("10");
    private static final BigDecimal BUBBLE_WRAP_ADDITIONAL_KG = new BigDecimal("1.5");

    /**
     * Tính phí dịch vụ (Service Fee) - 1.5% trên giá trị sản phẩm
     *
     * Phí này được tính tự động khi checkout và đã bao gồm trong deposit 70%
     *
     * CÔNG THỨC:
     * Service Fee = Product Cost x 1.5 / 100
     *
     * VÍ DỤ:
     * - Product Cost = 10,000,000đ
     * - Service Fee = 10,000,000 x 1.5 / 100 = 150,000đ
     *
     * ROUNDING:
     * - Làm tròn HALF_UP về số nguyên (không có lẻ)
     * - 150,500đ → 151,000đ
     * - 150,499đ → 150,000đ
     *
     * @param productCost Tổng giá trị sản phẩm (VND)
     * @return Phí dịch vụ (VND, số nguyên)
     */
    public BigDecimal calculateServiceFee(BigDecimal productCost) {
        if (productCost == null || productCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return productCost
                .multiply(SERVICE_FEE_PERCENTAGE)
                .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
    }

    /**
     * Tính phí kiểm đếm hàng (Item Count Check Fee)
     *
     * Dịch vụ kiểm đếm đảm bảo số lượng hàng đúng như order
     * Phí tính theo số lượng items và loại hàng (regular/accessory)
     *
     * LOGIC TÍNH PHÍ:
     * 1. Đếm tổng số items trong order
     * 2. Classify mỗi item là regular hoặc accessory (< 10 CNY)
     * 3. Tính phí riêng cho regular items và accessory items
     * 4. Cộng tổng phí
     *
     * ACCESSORY DETECTION:
     * - Convert item price về CNY (nếu là USD)
     * - Nếu price < 10 CNY → classify là accessory
     * - Accessory items có phí rẻ hơn (nhỏ, dễ đếm)
     *
     * TIER-BASED PRICING:
     * Regular Items:
     * - 1-5: 5,000đ/item
     * - 6-20: 3,000đ/item
     * - 21-100: 2,000đ/item
     * - 101-500: 1,500đ/item
     * - 501+: 1,000đ/item
     *
     * Accessory Items (< 10 CNY):
     * - 1-5: 2,500đ/item
     * - 6-20: 2,000đ/item
     * - 21-100: 1,500đ/item
     * - 101-500: 1,000đ/item
     * - 501+: 800đ/item
     *
     * VÍ DỤ:
     * Order có:
     * - 10 regular items (giá > 10 CNY)
     * - 5 accessory items (giá < 10 CNY)
     *
     * Phí:
     * - Regular: 10 x 3,000đ = 30,000đ (tier 6-20)
     * - Accessory: 5 x 2,500đ = 12,500đ (tier 1-5)
     * - Total: 42,500đ
     *
     * @param orderItems List các OrderItem cần kiểm đếm
     * @return Phí kiểm đếm hàng (VND)
     */
    public BigDecimal calculateItemCountCheckFee(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int totalItems = 0;
        int accessoryItems = 0;

        // Count total items and accessory items
        for (OrderItem item : orderItems) {
            int quantity = item.getQuantity();
            totalItems += quantity;

            // Check if item is accessory (price < 10 CNY)
            BigDecimal priceInCNY = item.getPrice();
            String currency = inferCurrency(item.getMarketplace());

            // Convert to CNY if needed
            if (!"CNY".equals(currency)) {
                priceInCNY = exchangeRateService.convertCurrency(
                        priceInCNY,
                        currency,
                        "CNY"
                );
            }

            if (priceInCNY != null && priceInCNY.compareTo(ACCESSORY_PRICE_THRESHOLD) < 0) {
                accessoryItems += quantity;
            }
        }

        int regularItems = totalItems - accessoryItems;

        // Calculate fee for regular items
        BigDecimal regularFee = calculateFeeByQuantity(regularItems, false);

        // Calculate fee for accessory items
        BigDecimal accessoryFee = calculateFeeByQuantity(accessoryItems, true);

        log.info("Item count check fee calculated: {} regular items ({}đ), {} accessory items ({}đ), total: {}đ",
                regularItems, regularFee, accessoryItems, accessoryFee, regularFee.add(accessoryFee));

        return regularFee.add(accessoryFee);
    }

    /**
     * Infer currency from marketplace
     * @param marketplace Marketplace enum
     * @return Currency code (USD for ALIEXPRESS, CNY for ALIBABA1688)
     */
    private String inferCurrency(Marketplace marketplace) {
        if (marketplace == null) {
            return "USD"; // Default to USD
        }

        if (marketplace == Marketplace.ALIEXPRESS) {
            return "USD";
        } else if (marketplace == Marketplace.ALIBABA1688) {
            return "CNY";
        }

        return "USD"; // Default fallback
    }

    /**
     * Calculate fee based on quantity tier
     * @param quantity Number of items
     * @param isAccessory True if accessory items
     * @return Fee in VND
     */
    private BigDecimal calculateFeeByQuantity(int quantity, boolean isAccessory) {
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal feePerItem;

        if (quantity >= 501) {
            feePerItem = isAccessory ? ACCESSORY_FEE_501_10000 : ITEM_COUNT_FEE_501_10000;
        } else if (quantity >= 101) {
            feePerItem = isAccessory ? ACCESSORY_FEE_101_500 : ITEM_COUNT_FEE_101_500;
        } else if (quantity >= 21) {
            feePerItem = isAccessory ? ACCESSORY_FEE_21_100 : ITEM_COUNT_FEE_21_100;
        } else if (quantity >= 6) {
            feePerItem = isAccessory ? ACCESSORY_FEE_6_20 : ITEM_COUNT_FEE_6_20;
        } else {
            feePerItem = isAccessory ? ACCESSORY_FEE_1_5 : ITEM_COUNT_FEE_1_5;
        }

        return feePerItem.multiply(new BigDecimal(quantity));
    }

    /**
     * Tính phí đóng gỗ (Wooden Packaging Fee)
     *
     * Dịch vụ đóng gỗ bảo vệ hàng dễ vỡ (đồ gốm, kính, điện tử, v.v.)
     * Phí tính bằng CNY, tự động convert sang VND
     *
     * CÔNG THỨC:
     * - Kg đầu tiên: 20 CNY
     * - Mỗi kg thêm: 1 CNY/kg
     *
     * VÍ DỤ:
     * - 1 kg: 20 CNY
     * - 5 kg: 20 + (5-1)*1 = 24 CNY
     * - 10 kg: 20 + (10-1)*1 = 29 CNY
     *
     * CONVERT VÀ RETURN:
     * - Fee tính bằng CNY
     * - Convert CNY → VND qua ExchangeRateService
     * - Return fee bằng VND
     *
     * @param weightKg Trọng lượng hàng (kg)
     * @return Phí đóng gỗ (VND)
     */
    public BigDecimal calculateWoodenPackagingFee(BigDecimal weightKg) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // First kg: 20 CNY
        BigDecimal fee = WOODEN_PACKAGING_FIRST_KG;

        // Additional kg: 1 CNY per kg
        if (weightKg.compareTo(BigDecimal.ONE) > 0) {
            BigDecimal additionalKg = weightKg.subtract(BigDecimal.ONE);
            fee = fee.add(additionalKg.multiply(WOODEN_PACKAGING_ADDITIONAL_KG));
        }

        // Convert CNY to VND
        BigDecimal feeInVND = exchangeRateService.convertCurrency(fee, "CNY", "VND");

        log.info("Wooden packaging fee calculated: {}kg = {} CNY = {}đ", weightKg, fee, feeInVND);

        return feeInVND;
    }

    /**
     * Tính phí bọc bong bóng (Bubble Wrap Fee)
     *
     * Dịch vụ bọc bong bóng chống va đập cho hàng
     * Phí tính bằng CNY, tự động convert sang VND
     *
     * CÔNG THỨC:
     * - Kg đầu tiên: 10 CNY
     * - Mỗi kg thêm: 1.5 CNY/kg
     *
     * VÍ DỤ:
     * - 1 kg: 10 CNY
     * - 5 kg: 10 + (5-1)*1.5 = 16 CNY
     * - 10 kg: 10 + (10-1)*1.5 = 23.5 CNY
     *
     * SO SÁNH VỚI WOODEN PACKAGING:
     * - Bubble wrap rẻ hơn (10 CNY vs 20 CNY cho kg đầu)
     * - Nhưng kg thêm đắt hơn (1.5 CNY vs 1 CNY)
     * - Phù hợp với hàng nhẹ, cần protection vừa phải
     *
     * CONVERT VÀ RETURN:
     * - Fee tính bằng CNY
     * - Convert CNY → VND qua ExchangeRateService
     * - Return fee bằng VND
     *
     * @param weightKg Trọng lượng hàng (kg)
     * @return Phí bọc bong bóng (VND)
     */
    public BigDecimal calculateBubbleWrapFee(BigDecimal weightKg) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // First kg: 10 CNY
        BigDecimal fee = BUBBLE_WRAP_FIRST_KG;

        // Additional kg: 1.5 CNY per kg
        if (weightKg.compareTo(BigDecimal.ONE) > 0) {
            BigDecimal additionalKg = weightKg.subtract(BigDecimal.ONE);
            fee = fee.add(additionalKg.multiply(BUBBLE_WRAP_ADDITIONAL_KG));
        }

        // Convert CNY to VND
        BigDecimal feeInVND = exchangeRateService.convertCurrency(fee, "CNY", "VND");

        log.info("Bubble wrap fee calculated: {}kg = {} CNY = {}đ", weightKg, fee, feeInVND);

        return feeInVND;
    }

    /**
     * Tính tổng phí dịch vụ thêm (Total Additional Services Fee)
     *
     * Method này tổng hợp tất cả các phí dịch vụ thêm theo yêu cầu của admin
     *
     * ĐƯỢC GỌI BỞI:
     * - OrderService.updateOrderFees() khi admin cập nhật phí sau khi order hàng
     *
     * LUỒNG XỬ LÝ:
     * 1. Admin order hàng từ marketplace (AliExpress/1688)
     * 2. Hàng về kho Trung Quốc
     * 3. Admin kiểm tra hàng, cân nặng, chọn services cần thiết:
     *    - Item count check: Có cần kiểm đếm không?
     *    - Wooden packaging: Hàng dễ vỡ cần đóng gỗ không?
     *    - Bubble wrap: Cần bọc bong bóng không?
     * 4. Admin nhập weight ước tính và chọn services
     * 5. System gọi method này để tính tổng phí
     * 6. Cộng vào remaining amount cho user thanh toán
     *
     * SERVICES INCLUDED (Optional):
     * - Item Count Check: Phí kiểm đếm hàng theo số lượng
     * - Wooden Packaging: Phí đóng gỗ theo trọng lượng
     * - Bubble Wrap: Phí bọc bong bóng theo trọng lượng
     *
     * VÍ DỤ:
     * Order có 20 regular items, weight = 5kg
     * Admin chọn: Item count check + Bubble wrap
     *
     * Tính phí:
     * - Item count check: 20 items x 3,000đ = 60,000đ (tier 6-20)
     * - Bubble wrap: 10 + (5-1)*1.5 = 16 CNY ≈ 60,000đ (tỷ giá ~3,750đ/CNY)
     * - Total: 120,000đ
     *
     * → User cần trả thêm 120,000đ (remaining amount tăng lên)
     *
     * @param orderItems List các OrderItem (để tính item count check)
     * @param weightKg Trọng lượng ước tính (kg, để tính packaging fees)
     * @param includeWoodenPackaging true nếu cần đóng gỗ
     * @param includeBubbleWrap true nếu cần bọc bong bóng
     * @param includeItemCountCheck true nếu cần kiểm đếm
     * @return Tổng phí dịch vụ thêm (VND)
     */
    public BigDecimal calculateTotalAdditionalServicesFee(
            List<OrderItem> orderItems,
            BigDecimal weightKg,
            boolean includeWoodenPackaging,
            boolean includeBubbleWrap,
            boolean includeItemCountCheck
    ) {
        BigDecimal total = BigDecimal.ZERO;

        if (includeItemCountCheck) {
            total = total.add(calculateItemCountCheckFee(orderItems));
        }

        if (includeWoodenPackaging) {
            total = total.add(calculateWoodenPackagingFee(weightKg));
        }

        if (includeBubbleWrap) {
            total = total.add(calculateBubbleWrapFee(weightKg));
        }

        return total;
    }
}
