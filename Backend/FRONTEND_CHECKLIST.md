# CHECKLIST KIỂM TRA VÀ FIX FRONTEND

**Ngày tạo:** 2025-10-28
**Mục đích:** Đảm bảo frontend tương thích với API pagination mới

---

## 📋 CHECKLIST KIỂM TRA

### ✅ BƯỚC 1: XÁC ĐỊNH API ENDPOINT ĐANG DÙNG

Kiểm tra frontend code để tìm các API calls:

#### 1.1. Search cho file gọi API AliExpress
```bash
# Tìm trong source code
grep -r "aliexpress/search" src/
grep -r "framePosition" src/
grep -r "frameSize" src/
```

**Các file cần kiểm tra:**
- [ ] Components: `ProductList.jsx`, `ProductGrid.jsx`, `SearchPage.jsx`
- [ ] Services/API: `apiService.js`, `productService.js`, `aliexpressService.js`
- [ ] Hooks: `useProducts.js`, `useSearch.js`, `usePagination.js`
- [ ] Utils: `api.js`, `axios.config.js`, `fetch.utils.js`

#### 1.2. Check API endpoints hiện tại

**Tìm trong code những patterns này:**

```javascript
// ❌ CŨ - Cần sửa
fetch('/api/aliexpress/search?framePosition=...')
fetch('/api/aliexpress/search/simple?framePosition=...')
fetch('/api/1688/search?framePosition=...')
fetch('/api/1688/search/simple?framePosition=...')

axios.get('/api/aliexpress/search/simple', {
  params: { framePosition, frameSize }
})
```

**Ghi chú vị trí tìm thấy:**
```
File: _______________________________
Dòng: _______________________________
Endpoint: ___________________________
```

---

### ✅ BƯỚC 2: KIỂM TRA PAGINATION LOGIC

#### 2.1. Tìm component xử lý pagination

**React Example:**
```javascript
// File: components/Pagination.jsx hoặc hooks/usePagination.js
const [currentPage, setCurrentPage] = useState(1);
const [framePosition, setFramePosition] = useState(0);
```

#### 2.2. Kiểm tra cách tính framePosition

**❌ CÁCH CŨ (SAI):**
```javascript
// Có thể frontend đang làm vậy
const framePosition = currentPage;  // ← SAI! Gửi 1, 2, 3...

// Hoặc
const framePosition = currentPage - 1;  // ← Vẫn SAI! Gửi 0, 1, 2...
```

**✅ CÁCH MỚI (ĐÚNG - Backend đã xử lý):**
```javascript
// Frontend chỉ cần gửi page number
const page = currentPage;  // Gửi 1, 2, 3...
```

---

### ✅ BƯỚC 3: KIỂM TRA STATE MANAGEMENT

#### 3.1. Redux/Context/Zustand State

**Tìm state liên quan đến pagination:**
```javascript
// Trong Redux store / Context / Zustand
const initialState = {
  products: [],
  currentPage: 1,          // ✅ OK
  framePosition: 0,        // ❌ Không cần nữa!
  frameSize: 12,           // ✅ Đổi tên → pageSize
  totalPages: 0,
  loading: false,
  error: null
}
```

**Checklist:**
- [ ] Có state `framePosition` không? → Cần xóa hoặc đổi tên
- [ ] Có state `frameSize` không? → Đổi tên thành `pageSize`
- [ ] Có state `currentPage` không? → Giữ nguyên
- [ ] Có logic tính toán offset không? → Xóa đi (backend đã xử lý)

---

### ✅ BƯỚC 4: KIỂM TRA API CALL FUNCTIONS

#### 4.1. Tìm function fetch products

**Pattern cần tìm:**
```javascript
// apiService.js hoặc productService.js
export const searchProducts = async (keyword, framePosition, frameSize) => {
  // ...
}

export const getProducts = (keyword, page, pageSize) => {
  // ...
}
```

#### 4.2. Kiểm tra parameters

**❌ CÁCH CŨ (CẦN SỬA):**
```javascript
export const searchAliExpressProducts = async (keyword, framePosition = 0, frameSize = 12) => {
  const response = await axios.get('/api/aliexpress/search/simple', {
    params: {
      keyword,
      framePosition,   // ❌ Parameter cũ
      frameSize        // ❌ Parameter cũ
    }
  });
  return response.data;
}
```

**✅ CÁCH MỚI (ĐÚNG):**
```javascript
export const searchAliExpressProducts = async (keyword, page = 1, pageSize = 12) => {
  const response = await axios.get('/api/aliexpress/search/simple', {
    params: {
      keyword,
      page,       // ✅ Parameter mới (1-indexed)
      pageSize    // ✅ Parameter mới
    }
  });
  return response.data;
}
```

---

### ✅ BƯỚC 5: KIỂM TRA PAGINATION COMPONENTS

#### 5.1. Button handlers

**❌ CÁCH CŨ:**
```javascript
const handleNextPage = () => {
  const newFramePosition = framePosition + frameSize;
  setFramePosition(newFramePosition);
  fetchProducts(keyword, newFramePosition, frameSize);
}

const handlePrevPage = () => {
  const newFramePosition = Math.max(0, framePosition - frameSize);
  setFramePosition(newFramePosition);
  fetchProducts(keyword, newFramePosition, frameSize);
}
```

**✅ CÁCH MỚI:**
```javascript
const handleNextPage = () => {
  const newPage = currentPage + 1;
  setCurrentPage(newPage);
  fetchProducts(keyword, newPage, pageSize);
}

const handlePrevPage = () => {
  const newPage = Math.max(1, currentPage - 1);
  setCurrentPage(newPage);
  fetchProducts(keyword, newPage, pageSize);
}

const handlePageClick = (pageNumber) => {
  setCurrentPage(pageNumber);
  fetchProducts(keyword, pageNumber, pageSize);
}
```

---

## 🔧 CÁC VIỆC CẦN LÀM (THEO THỨ TỰ ƯU TIÊN)

### TASK 1: Update API Service Layer (HIGH PRIORITY)

**File cần sửa:** `src/services/apiService.js` hoặc `src/api/products.js`

**Công việc:**

1. **Đổi tên parameters trong function signature:**
```javascript
// ❌ TRƯỚC
export const searchProducts = async (keyword, framePosition, frameSize) => { ... }

// ✅ SAU
export const searchProducts = async (keyword, page, pageSize) => { ... }
```

2. **Update API calls:**
```javascript
// ❌ TRƯỚC
const response = await fetch(
  `/api/aliexpress/search/simple?keyword=${keyword}&framePosition=${framePosition}&frameSize=${frameSize}`
);

// ✅ SAU
const response = await fetch(
  `/api/aliexpress/search/simple?keyword=${keyword}&page=${page}&pageSize=${pageSize}`
);
```

3. **Nếu dùng axios:**
```javascript
// ❌ TRƯỚC
axios.get('/api/aliexpress/search/simple', {
  params: { keyword, framePosition, frameSize }
})

// ✅ SAU
axios.get('/api/aliexpress/search/simple', {
  params: { keyword, page, pageSize }
})
```

**Code mẫu hoàn chỉnh:**
```javascript
// src/services/productService.js

// AliExpress Search
export const searchAliExpressProducts = async (keyword, page = 1, pageSize = 12, language = 'en') => {
  try {
    const response = await axios.get('/api/aliexpress/search/simple', {
      params: {
        keyword,
        page,
        pageSize,
        language
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error searching AliExpress products:', error);
    throw error;
  }
};

// Alibaba 1688 Search
export const search1688Products = async (keyword, page = 1, pageSize = 12, language = 'en') => {
  try {
    const response = await axios.get('/api/1688/search/simple', {
      params: {
        keyword,
        page,
        pageSize,
        language
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error searching 1688 products:', error);
    throw error;
  }
};

// Get product details
export const getProductDetails = async (marketplace, productId) => {
  const endpoint = marketplace === 'aliexpress'
    ? `/api/aliexpress/products/${productId}/simple`
    : `/api/1688/products/${productId}`;

  try {
    const response = await axios.get(endpoint);
    return response.data;
  } catch (error) {
    console.error('Error fetching product details:', error);
    throw error;
  }
};
```

---

### TASK 2: Update State Management (HIGH PRIORITY)

**File cần sửa:** Redux store / Context / Zustand state files

#### Option A: Redux (productSlice.js)

```javascript
// src/store/slices/productSlice.js

const initialState = {
  products: [],
  currentPage: 1,        // ✅ Giữ nguyên
  pageSize: 12,          // ✅ Đổi từ frameSize
  totalPages: 0,
  totalItems: 0,
  loading: false,
  error: null,
  searchKeyword: ''
};

// Actions
export const fetchProducts = createAsyncThunk(
  'products/fetch',
  async ({ keyword, page, pageSize }) => {
    const data = await searchAliExpressProducts(keyword, page, pageSize);
    return data;
  }
);

// Reducers
const productSlice = createSlice({
  name: 'products',
  initialState,
  reducers: {
    setCurrentPage: (state, action) => {
      state.currentPage = action.payload;
    },
    setPageSize: (state, action) => {
      state.pageSize = action.payload;
      state.currentPage = 1; // Reset về page 1 khi đổi pageSize
    },
    resetPagination: (state) => {
      state.currentPage = 1;
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchProducts.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchProducts.fulfilled, (state, action) => {
        state.loading = false;
        state.products = action.payload.products;
        state.totalPages = action.payload.meta?.totalPages || 0;
        state.totalItems = action.payload.meta?.totalResults || 0;
      })
      .addCase(fetchProducts.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message;
      });
  }
});

export const { setCurrentPage, setPageSize, resetPagination } = productSlice.actions;
export default productSlice.reducer;
```

#### Option B: React Context

```javascript
// src/context/ProductContext.jsx

import { createContext, useContext, useState } from 'react';
import { searchAliExpressProducts } from '../services/productService';

const ProductContext = createContext();

export const ProductProvider = ({ children }) => {
  const [products, setProducts] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(12);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchProducts = async (keyword, page, size) => {
    setLoading(true);
    setError(null);

    try {
      const data = await searchAliExpressProducts(keyword, page, size);
      setProducts(data.products || []);
      setTotalPages(data.meta?.totalPages || 0);
      setCurrentPage(page);
      setPageSize(size);
    } catch (err) {
      setError(err.message);
      console.error('Error fetching products:', err);
    } finally {
      setLoading(false);
    }
  };

  const goToPage = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  const nextPage = () => {
    if (currentPage < totalPages) {
      setCurrentPage(prev => prev + 1);
    }
  };

  const prevPage = () => {
    if (currentPage > 1) {
      setCurrentPage(prev => prev - 1);
    }
  };

  const value = {
    products,
    currentPage,
    pageSize,
    totalPages,
    loading,
    error,
    fetchProducts,
    goToPage,
    nextPage,
    prevPage,
    setPageSize
  };

  return (
    <ProductContext.Provider value={value}>
      {children}
    </ProductContext.Provider>
  );
};

export const useProducts = () => {
  const context = useContext(ProductContext);
  if (!context) {
    throw new Error('useProducts must be used within ProductProvider');
  }
  return context;
};
```

---

### TASK 3: Update Components (MEDIUM PRIORITY)

**File cần sửa:** Product list components, Search page components

#### 3.1. Product List Component

```javascript
// src/components/ProductList.jsx

import React, { useEffect } from 'react';
import { useProducts } from '../context/ProductContext';
// hoặc
// import { useDispatch, useSelector } from 'react-redux';
// import { fetchProducts } from '../store/slices/productSlice';

const ProductList = ({ keyword }) => {
  const {
    products,
    currentPage,
    pageSize,
    totalPages,
    loading,
    error,
    fetchProducts
  } = useProducts();

  // Fetch products khi keyword hoặc page thay đổi
  useEffect(() => {
    if (keyword) {
      fetchProducts(keyword, currentPage, pageSize);
    }
  }, [keyword, currentPage, pageSize]);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div className="product-list">
      <div className="products-grid">
        {products.map(product => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>

      <Pagination
        currentPage={currentPage}
        totalPages={totalPages}
      />
    </div>
  );
};

export default ProductList;
```

#### 3.2. Pagination Component

```javascript
// src/components/Pagination.jsx

import React from 'react';
import { useProducts } from '../context/ProductContext';

const Pagination = ({ currentPage, totalPages }) => {
  const { goToPage, nextPage, prevPage } = useProducts();

  // Tạo array các page numbers để hiển thị
  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5; // Hiển thị tối đa 5 page numbers

    let startPage = Math.max(1, currentPage - Math.floor(maxVisible / 2));
    let endPage = Math.min(totalPages, startPage + maxVisible - 1);

    if (endPage - startPage < maxVisible - 1) {
      startPage = Math.max(1, endPage - maxVisible + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  };

  return (
    <div className="pagination">
      {/* Previous button */}
      <button
        onClick={prevPage}
        disabled={currentPage === 1}
        className="pagination-btn"
      >
        Previous
      </button>

      {/* First page */}
      {currentPage > 3 && (
        <>
          <button onClick={() => goToPage(1)} className="pagination-btn">
            1
          </button>
          <span className="pagination-dots">...</span>
        </>
      )}

      {/* Page numbers */}
      {getPageNumbers().map(pageNum => (
        <button
          key={pageNum}
          onClick={() => goToPage(pageNum)}
          className={`pagination-btn ${currentPage === pageNum ? 'active' : ''}`}
        >
          {pageNum}
        </button>
      ))}

      {/* Last page */}
      {currentPage < totalPages - 2 && (
        <>
          <span className="pagination-dots">...</span>
          <button onClick={() => goToPage(totalPages)} className="pagination-btn">
            {totalPages}
          </button>
        </>
      )}

      {/* Next button */}
      <button
        onClick={nextPage}
        disabled={currentPage === totalPages}
        className="pagination-btn"
      >
        Next
      </button>

      {/* Page info */}
      <span className="pagination-info">
        Page {currentPage} of {totalPages}
      </span>
    </div>
  );
};

export default Pagination;
```

---

### TASK 4: Update Custom Hooks (MEDIUM PRIORITY)

**File cần sửa:** `src/hooks/useProducts.js`, `src/hooks/usePagination.js`

```javascript
// src/hooks/useProducts.js

import { useState, useEffect, useCallback } from 'react';
import { searchAliExpressProducts } from '../services/productService';

export const useProducts = (initialKeyword = '', initialPageSize = 12) => {
  const [products, setProducts] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [keyword, setKeyword] = useState(initialKeyword);

  const fetchProducts = useCallback(async (searchKeyword, page, size) => {
    if (!searchKeyword) return;

    setLoading(true);
    setError(null);

    try {
      const data = await searchAliExpressProducts(searchKeyword, page, size);

      setProducts(data.products || []);
      setTotalPages(data.meta?.totalPages || 0);
      setTotalItems(data.meta?.totalResults || 0);
      setCurrentPage(page);

    } catch (err) {
      setError(err.message);
      console.error('Error fetching products:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  // Auto fetch khi keyword, currentPage, hoặc pageSize thay đổi
  useEffect(() => {
    if (keyword) {
      fetchProducts(keyword, currentPage, pageSize);
    }
  }, [keyword, currentPage, pageSize, fetchProducts]);

  const goToPage = useCallback((pageNumber) => {
    if (pageNumber >= 1 && pageNumber <= totalPages) {
      setCurrentPage(pageNumber);
    }
  }, [totalPages]);

  const nextPage = useCallback(() => {
    if (currentPage < totalPages) {
      setCurrentPage(prev => prev + 1);
    }
  }, [currentPage, totalPages]);

  const prevPage = useCallback(() => {
    if (currentPage > 1) {
      setCurrentPage(prev => prev - 1);
    }
  }, [currentPage]);

  const search = useCallback((newKeyword) => {
    setKeyword(newKeyword);
    setCurrentPage(1); // Reset về page 1 khi search mới
  }, []);

  const changePageSize = useCallback((newSize) => {
    setPageSize(newSize);
    setCurrentPage(1); // Reset về page 1 khi đổi page size
  }, []);

  return {
    products,
    currentPage,
    pageSize,
    totalPages,
    totalItems,
    loading,
    error,
    keyword,
    fetchProducts,
    goToPage,
    nextPage,
    prevPage,
    search,
    changePageSize
  };
};
```

---

### TASK 5: Update URL Query Parameters (LOW PRIORITY - Optional)

**Mục đích:** Sync pagination với URL để user có thể bookmark/share

```javascript
// src/hooks/useSearchParams.js

import { useSearchParams } from 'react-router-dom';
import { useEffect } from 'react';

export const useProductSearchParams = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const keyword = searchParams.get('keyword') || '';
  const page = parseInt(searchParams.get('page')) || 1;
  const pageSize = parseInt(searchParams.get('pageSize')) || 12;

  const updateSearchParams = (newKeyword, newPage, newPageSize) => {
    const params = new URLSearchParams();

    if (newKeyword) params.set('keyword', newKeyword);
    if (newPage) params.set('page', newPage);
    if (newPageSize && newPageSize !== 12) params.set('pageSize', newPageSize);

    setSearchParams(params);
  };

  return {
    keyword,
    page,
    pageSize,
    updateSearchParams
  };
};
```

**Sử dụng trong component:**
```javascript
const SearchPage = () => {
  const { keyword, page, pageSize, updateSearchParams } = useProductSearchParams();
  const { products, loading, fetchProducts } = useProducts();

  useEffect(() => {
    if (keyword) {
      fetchProducts(keyword, page, pageSize);
    }
  }, [keyword, page, pageSize]);

  const handlePageChange = (newPage) => {
    updateSearchParams(keyword, newPage, pageSize);
  };

  return (
    // ... component JSX
  );
};
```

---

## 🧪 TESTING CHECKLIST

### Manual Testing:

- [ ] **Test Page 1:**
  - [ ] Load trang đầu tiên → Hiển thị 12 sản phẩm đầu tiên
  - [ ] Check console network tab → API call có `page=1&pageSize=12`

- [ ] **Test Page 2:**
  - [ ] Click "Next" hoặc page 2 → Hiển thị 12 sản phẩm KHÁC
  - [ ] Check console → API call có `page=2&pageSize=12`
  - [ ] Verify: Không có product ID nào trùng với page 1

- [ ] **Test Page 3:**
  - [ ] Click page 3 → Hiển thị 12 sản phẩm KHÁC với page 1, 2
  - [ ] Check console → API call có `page=3&pageSize=12`

- [ ] **Test Previous button:**
  - [ ] Từ page 3 → page 2 → Hiển thị lại đúng sản phẩm của page 2

- [ ] **Test Direct page navigation:**
  - [ ] Click vào page number trực tiếp (e.g., page 5)
  - [ ] Verify hiển thị đúng sản phẩm của page đó

- [ ] **Test page size change:**
  - [ ] Đổi từ 12 items → 24 items per page
  - [ ] Verify API call có `pageSize=24`
  - [ ] Verify hiển thị đúng 24 items

- [ ] **Test search với keyword mới:**
  - [ ] Nhập keyword mới → Verify reset về page 1
  - [ ] Check API call có `page=1`

### Automated Testing (Optional):

```javascript
// Example test with Jest + React Testing Library

describe('Product Pagination', () => {
  it('should fetch page 1 on initial load', async () => {
    render(<ProductList keyword="phone" />);

    await waitFor(() => {
      expect(screen.getByText(/page 1/i)).toBeInTheDocument();
    });
  });

  it('should fetch page 2 when clicking next', async () => {
    const { container } = render(<ProductList keyword="phone" />);

    const nextButton = screen.getByText(/next/i);
    fireEvent.click(nextButton);

    await waitFor(() => {
      expect(screen.getByText(/page 2/i)).toBeInTheDocument();
    });
  });

  it('should call API with correct page parameter', async () => {
    const mockFetch = jest.spyOn(global, 'fetch');

    render(<ProductList keyword="phone" />);

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('page=1&pageSize=12')
      );
    });
  });
});
```

---

## 📝 EXAMPLE CODE STRUCTURE

### Recommended file structure:

```
src/
├── api/
│   └── productService.js          ← TASK 1
├── store/
│   └── slices/
│       └── productSlice.js        ← TASK 2 (Redux)
├── context/
│   └── ProductContext.jsx         ← TASK 2 (Context)
├── hooks/
│   ├── useProducts.js             ← TASK 4
│   └── useSearchParams.js         ← TASK 5
├── components/
│   ├── ProductList.jsx            ← TASK 3
│   ├── ProductCard.jsx
│   ├── Pagination.jsx             ← TASK 3
│   └── SearchBar.jsx
└── pages/
    └── SearchPage.jsx
```

---

## ⚠️ COMMON PITFALLS (Những lỗi thường gặp)

### 1. Quên reset page về 1 khi search mới
```javascript
// ❌ SAI
const handleSearch = (newKeyword) => {
  setKeyword(newKeyword);
  fetchProducts(newKeyword, currentPage, pageSize); // ← Giữ nguyên currentPage
}

// ✅ ĐÚNG
const handleSearch = (newKeyword) => {
  setKeyword(newKeyword);
  setCurrentPage(1); // ← Reset về page 1
  fetchProducts(newKeyword, 1, pageSize);
}
```

### 2. Mix page và framePosition
```javascript
// ❌ SAI - Vẫn còn tính framePosition
const framePosition = (page - 1) * pageSize;
fetchProducts(keyword, framePosition, pageSize); // ← Sai parameter

// ✅ ĐÚNG - Chỉ gửi page
fetchProducts(keyword, page, pageSize);
```

### 3. Page 0-indexed vs 1-indexed
```javascript
// ❌ SAI - Page bắt đầu từ 0
const [currentPage, setCurrentPage] = useState(0);

// ✅ ĐÚNG - Page bắt đầu từ 1
const [currentPage, setCurrentPage] = useState(1);
```

### 4. Không update dependencies trong useEffect
```javascript
// ❌ SAI
useEffect(() => {
  fetchProducts(keyword, currentPage, pageSize);
}, []); // ← Missing dependencies

// ✅ ĐÚNG
useEffect(() => {
  fetchProducts(keyword, currentPage, pageSize);
}, [keyword, currentPage, pageSize]); // ← Include all dependencies
```

---

## 🎯 SUMMARY CHECKLIST

### Phase 1: Investigation
- [ ] Tìm tất cả API calls liên quan đến product search
- [ ] Xác định state management approach (Redux/Context/Local)
- [ ] List ra tất cả components sử dụng pagination

### Phase 2: Code Changes
- [ ] TASK 1: Update API Service Layer
- [ ] TASK 2: Update State Management
- [ ] TASK 3: Update Components
- [ ] TASK 4: Update Custom Hooks (if any)
- [ ] TASK 5: Update URL params (optional)

### Phase 3: Testing
- [ ] Test pagination trên development environment
- [ ] Verify không còn lặp sản phẩm
- [ ] Test all edge cases (first page, last page, direct navigation)
- [ ] Cross-browser testing

### Phase 4: Deployment
- [ ] Code review
- [ ] Merge to main branch
- [ ] Deploy to staging
- [ ] Final testing on staging
- [ ] Deploy to production

---

## 📞 SUPPORT

Nếu gặp vấn đề khi implement:

1. Check backend logs: `./mvnw spring-boot:run`
2. Check browser console: Network tab → XHR requests
3. Verify API response format matches expected structure
4. Test API directly với curl/Postman trước khi test qua frontend

---

**Document Version:** 1.0
**Last Updated:** 2025-10-28
**Status:** Ready for Frontend Team
