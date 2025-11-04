import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { productService } from '../services/productService';
import { addToCart } from '../../cart/services';
import { useCart } from '../../cart/context/CartContext';
import { useCurrency } from '../../currency/context/CurrencyContext';
import ProductImages from '../components/ProductImages';
import ProductInfo from '../components/ProductInfo';
import ProductVariantSelector from '../components/ProductVariantSelector';
import ProductSKU from '../components/ProductSKU';
import ProductShipping from '../components/ProductShipping';
import ProductSeller from '../components/ProductSeller';
import ProductAttributes from '../components/ProductAttributes';
import Loading from '../../../shared/components/Loading';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import { Shield, Truck, ShoppingCart } from 'lucide-react';
import { isValidMarketplace, getMarketplaceInfo } from '../../../shared/utils/marketplace';

const ProductDetailPage = () => {
  const { platform, id } = useParams();
  const { refreshCart } = useCart();
  const { formatPrice } = useCurrency();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedVariant, setSelectedVariant] = useState(null);
  const [currentImages, setCurrentImages] = useState([]);
  const [currentPrice, setCurrentPrice] = useState(null);
  const [addingToCart, setAddingToCart] = useState(false);

  const fetchProduct = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Validate platform and id
      if (!platform || !id) {
        throw new Error('VALIDATION_ERROR: Platform and ID are required');
      }

      // Validate platform value
      if (!isValidMarketplace(platform)) {
        throw new Error(`VALIDATION_ERROR: Invalid platform "${platform}". Must be one of: aliexpress, 1688`);
      }

      // Fetch product using platform + id endpoint
      const data = await productService.getProductById(platform, id);
      setProduct(data);

      // Initialize images and price
      if (data.Result?.Item) {
        const item = data.Result.Item;
        setCurrentImages(item.Pictures?.map(p => p.Url) || []);
        setCurrentPrice(item.Price?.ConvertedPriceWithoutSign || item.Price?.OriginalPrice);
      }
    } catch (err) {
      console.error('Error fetching product:', err);

      // Enhance error message with provider info if available
      let errorMessage = err.message || err.toString();
      if (err.code && err.provider) {
        errorMessage = `${errorMessage} (Provider: ${err.provider})`;
      }

      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [platform, id]);

  useEffect(() => {
    fetchProduct();
  }, [fetchProduct]);

  // Handle variant selection changes
  const handleVariantChange = useCallback((variant) => {
    setSelectedVariant(variant);

    // Update price
    if (variant?.price) {
      setCurrentPrice(variant.price);
    }

    // Update images if variant has specific image
    if (variant?.variantImage && product?.Result?.Item) {
      const backendItem = product.Result.Item;

      // Find the full picture object for the variant image
      const variantPicture = backendItem.Pictures?.find(p =>
        p.Url === variant.variantImage || p.Url.includes(variant.variantImage)
      );

      if (variantPicture) {
        // Move variant image to front, keep other images
        const otherImages = backendItem.Pictures.filter(p => p.Url !== variantPicture.Url);
        setCurrentImages([variantPicture.Url, ...otherImages.map(p => p.Url)]);
      }
    }
  }, [product]);

  // Handle add to cart
  const handleAddToCart = useCallback(async () => {
    if (!product?.Result?.Item) return;

    try {
      const backendItem = product.Result.Item;
      const hasVariants = backendItem.Attributes?.some(attr => attr.IsConfigurator) || false;

      // Validation: Check if product has variants but none selected
      if (hasVariants && !selectedVariant) {
        toast.warning('Please select product options before adding to cart!');
        return;
      }

      setAddingToCart(true);

      // Get data from product
      const currency = backendItem.Price?.Currency || 'USD';

      // Transform variant data to backend format
      let variantData = null;
      if (selectedVariant && selectedVariant.selectedOptions) {
        // Create variantName from selected options values
        const variantName = Object.entries(selectedVariant.selectedOptions)
          .map(([pid, vid]) => {
            // Find the attribute to get property name and value
            const attr = backendItem.Attributes?.find(a => a.Pid === pid && a.Vid === vid);
            return attr ? attr.Value : vid;
          })
          .join(' - ');

        // Create variantOptions string in format "Property: Value, Property: Value"
        const variantOptions = Object.entries(selectedVariant.selectedOptions)
          .map(([pid, vid]) => {
            const attr = backendItem.Attributes?.find(a => a.Pid === pid && a.Vid === vid);
            return attr ? `${attr.PropertyName}: ${attr.Value}` : '';
          })
          .filter(Boolean)
          .join(', ');

        variantData = {
          variantId: selectedVariant.configuredItemId,
          variantName: variantName,
          variantOptions: variantOptions
        };
      }

      // Prepare cart item data
      const cartItem = {
        id: backendItem.Id,
        platform: platform,
        title: backendItem.Title,
        price: currentPrice || backendItem.Price?.ConvertedPriceWithoutSign || backendItem.Price?.OriginalPrice,
        currency: currency,
        image: currentImages?.[0] || backendItem.Pictures?.[0]?.Url || null,
        quantity: 1,
        // Send variant data in backend-expected format
        ...(variantData && {
          variantId: variantData.variantId,
          variantName: variantData.variantName,
          variantOptions: variantData.variantOptions
        })
      };

      console.log('🛒 Adding to cart (frontend data):', JSON.stringify(cartItem, null, 2));

      // User ID is automatically extracted from JWT token by backend
      const response = await addToCart(cartItem);

      // Debug: Log backend response after adding to cart
      console.log('✅ [ProductDetailPage] addToCart response:', response);

      // Refresh cart count in header
      refreshCart();

      toast.success('Product added to cart!');
    } catch (err) {
      console.error('❌ [ProductDetailPage] Error adding to cart:', err);

      // Handle specific error codes
      if (err.response?.status === 403) {
        toast.error('Please login to add items to cart');
      } else if (err.response?.status === 401) {
        toast.error('Session expired. Please login again');
      } else {
        toast.error(err.response?.data?.message || 'Failed to add product to cart');
      }
    } finally {
      setAddingToCart(false);
    }
  }, [product, platform, currentPrice, currentImages, selectedVariant, refreshCart]);

  if (loading) {
    return <Loading message="Loading product details..." />;
  }

  if (error) {
    return <ErrorMessage message={error} onRetry={fetchProduct} />;
  }

  if (!product || !product.Result || !product.Result.Item) {
    return <ErrorMessage message="Product not found" onRetry={fetchProduct} />;
  }

  const backendItem = product.Result.Item;
  const backendVendor = product.Result.Vendor;
  const backendRootPath = product.Result.RootPath;

  // Normalize backend response (Pascal case) to expected format (lowercase)
  const normalizedProduct = {
    result: {
      item: {
        title: backendItem.Title,
        itemId: backendItem.Id,
        images: currentImages.length > 0 ? currentImages : (backendItem.Pictures?.map(p => p.Url) || []),
        breadcrumbs: backendRootPath?.Content?.map(c => ({ title: c.Name })) || [],
        attributes: backendItem.Attributes || [],
        configuredItems: backendItem.ConfiguredItems || [],
        sku: {
          def: {
            price: currentPrice || backendItem.Price?.ConvertedPriceWithoutSign || backendItem.Price?.OriginalPrice,
            promotionPrice: backendItem.Promotions?.[0]?.Price?.ConvertedPriceWithoutSign,
            quantity: selectedVariant?.quantity || backendItem.MasterQuantity || 0,
            unit: 'piece',
            isBulk: false
          },
          list: backendItem.ConfiguredItems?.map(ci => ({
            id: ci.Id,
            price: ci.Price?.ConvertedPriceWithoutSign,
            quantity: ci.Quantity
          })) || []
        }
      },
      delivery: null, // Not present in backend response
      service: null, // Not present in backend response
      seller: backendVendor ? {
        storeTitle: backendVendor.Name || backendVendor.ShopName || backendVendor.DisplayName,
        storeId: backendVendor.Id,
        storeImage: backendVendor.DisplayPictureUrl?.replace('https:', ''),
        storeUrl: backendVendor.FeaturedValues?.find(f => f.Name === 'shopUrl')?.Value?.replace('https:', '') || '',
        rating: backendVendor.FeaturedValues?.find(f => f.Name === 'rating')?.Value,
        feedbackPercentage: backendVendor.FeaturedValues?.find(f => f.Name === 'feedbackPercentage')?.Value
      } : null
    }
  };

  const { item, delivery, service, seller } = normalizedProduct.result;

  // Get marketplace display info from product data (backend is source of truth)
  const marketplaceInfo = getMarketplaceInfo(product.platform || platform);

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Marketplace Badge */}
      {marketplaceInfo && (
        <div className="mb-4">
          <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${marketplaceInfo.colors.bg} ${marketplaceInfo.colors.text}`}>
            {marketplaceInfo.name}
          </span>
        </div>
      )}

      {/* Product Images and Info Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
        <div>
          <ProductImages images={item.images} title={item.title} />
        </div>

        <div className="space-y-6">
          <ProductInfo
            product={normalizedProduct}
            seller={seller}
            platform={platform}
            productId={backendItem?.Id}
          />

          {/* Product Variant Selector - Below Product Info */}
          {item.attributes && item.attributes.length > 0 && (
            <div className="bg-white rounded-lg p-6 shadow-sm">
              <h3 className="font-semibold text-gray-900 mb-4">Select Options</h3>
              <ProductVariantSelector
                attributes={item.attributes}
                configuredItems={item.configuredItems}
                onVariantChange={handleVariantChange}
              />

              {/* Add to Cart Button */}
              <div className="mt-6 pt-6 border-t border-gray-200">
                <button
                  onClick={handleAddToCart}
                  disabled={addingToCart}
                  className="w-full px-6 py-4 bg-red-600 text-white rounded-lg font-semibold hover:bg-red-700 transition-colors shadow-md hover:shadow-lg flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {addingToCart ? (
                    <>
                      <span>Adding...</span>
                    </>
                  ) : (
                    <>
                      <ShoppingCart className="w-5 h-5" />
                      <span>
                        Add to Cart - {
                          currentPrice && formatPrice
                            ? formatPrice(
                                parseFloat(currentPrice),
                                backendItem.Price?.Currency || (platform === '1688' ? 'CNY' : 'USD')
                              ).display
                            : 'N/A'
                        }
                      </span>
                    </>
                  )}
                </button>

                {selectedVariant && (
                  <p className="text-sm text-gray-600 mt-2 text-center">
                    {selectedVariant.quantity} pieces available
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
      
      {/* Service Guarantees Section */}
      {service && service.length > 0 && (
        <div className="mb-8">
          <div className="bg-gradient-to-r from-green-50 to-blue-50 rounded-lg p-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {service.map((item, index) => (
                <div key={index} className="flex items-start space-x-3">
                  {item.title && item.title.toLowerCase().includes('delivery') ? (
                    <Truck className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                  ) : (
                    <Shield className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                  )}
                  <div>
                    <h4 className="font-medium text-gray-900">{item.title}</h4>
                    <p className="text-sm text-gray-600">{item.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
      
      {/* SKU, Shipping, and Seller Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
        <div className="lg:col-span-2">
          <ProductSKU sku={item.sku} />
        </div>

        <div className="space-y-6">
          <ProductShipping delivery={delivery} />
          <ProductSeller seller={seller} />
        </div>
      </div>

      {/* Product Specifications (Non-Configurator Attributes) */}
      {item.attributes && item.attributes.filter(attr => !attr.IsConfigurator).length > 0 && (
        <div className="mb-8">
          <ProductAttributes attributes={item.attributes.filter(attr => !attr.IsConfigurator)} />
        </div>
      )}
      
      {/* Category Breadcrumbs Section */}
      {item.breadcrumbs && item.breadcrumbs.length > 0 && (
        <div className="mt-8 bg-white rounded-lg p-6 shadow-sm">
          <h3 className="font-semibold text-gray-900 mb-4">Category</h3>
          <div className="flex flex-wrap items-center gap-2">
            {item.breadcrumbs.map((crumb, index) => (
              <React.Fragment key={index}>
                {crumb.title && (
                  <>
                    <span className="text-sm text-gray-600">{crumb.title}</span>
                    {index < item.breadcrumbs.length - 1 && crumb.title && item.breadcrumbs[index + 1]?.title && (
                      <span className="text-gray-400">/</span>
                    )}
                  </>
                )}
              </React.Fragment>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductDetailPage;