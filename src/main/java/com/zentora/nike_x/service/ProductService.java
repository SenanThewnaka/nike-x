package com.zentora.nike_x.service;

import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.ProductDTO;
import com.zentora.nike_x.dto.SearchRequestDTO;
import com.zentora.nike_x.util.AppUtil;
import com.zentora.nike_x.util.HibernateUtil;
import org.hibernate.Session;

import com.zentora.nike_x.entity.Product;
import com.zentora.nike_x.entity.ProductImage;
import java.util.ArrayList;
import java.util.List;

public class ProductService {

    public String getHomeProducts() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        List<ProductDTO> productDTOs = new ArrayList<>();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

            // Query logic:
            // 1. Join Product with Stock.
            // 2. Filter where at least one stock has qty > 0.
            // 3. Order by Product ID DESC (Newest).
            // 4. Limit 8.

            // Since we need to aggregate or exist check, subquery is good.
            // "SELECT p FROM Product p WHERE (SELECT SUM(s.qty) FROM Stock s WHERE
            // s.product = p) > 0 ORDER BY p.id DESC"

            String hql = "SELECT p FROM Product p " +
                    "WHERE p.status.type = 'Active' AND (SELECT SUM(s.qty) FROM Stock s WHERE s.product = p AND s.status.type = 'Active') > 0 "
                    +
                    "ORDER BY p.id DESC";

            List<com.zentora.nike_x.entity.Product> products = hibernateSession
                    .createQuery(hql, com.zentora.nike_x.entity.Product.class)
                    .setMaxResults(8)
                    .list();

            for (com.zentora.nike_x.entity.Product product : products) {
                ProductDTO dto = new ProductDTO();
                dto.setId(product.getId());
                dto.setName(product.getName());
                dto.setBrandName(product.getBrand().getName());
                dto.setModelName(product.getModel().getName());
                dto.setGender(product.getGender().getName());

                // Get First Image
                String hqlImage = "SELECT p.path FROM ProductImage p WHERE p.product.id = :pid";
                List<String> images = hibernateSession.createQuery(hqlImage, String.class)
                        .setParameter("pid", product.getId())
                        .setMaxResults(1)
                        .list();
                if (!images.isEmpty()) {
                    dto.setImagePath(images.get(0));
                }

                // Get Lowest Active Price
                String hqlPrice = "SELECT MIN(s.sellingPrice) FROM Stock s WHERE s.product.id = :pid AND s.status.type = 'Active' AND s.qty > 0";
                Double minPrice = hibernateSession.createQuery(hqlPrice, Double.class)
                        .setParameter("pid", product.getId())
                        .uniqueResult();
                dto.setPrice(minPrice != null ? minPrice : 0.0);

                productDTOs.add(dto);
            }
            status = true;
            message = "Home products fetched successfully!";

            if (productDTOs.isEmpty()) {
                message = "No products found.";
            }

        } catch (Exception e) {
            message = "Error loading home products";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("products", AppUtil.GSON.toJsonTree(productDTOs));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getBrands() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> brandsData = hibernateSession.createQuery("SELECT b.id, b.name FROM Brand b", Object[].class)
                    .list();

            com.google.gson.JsonArray brands = new com.google.gson.JsonArray();
            for (Object[] row : brandsData) {
                JsonObject b = new JsonObject();
                b.addProperty("id", (Integer) row[0]);
                b.addProperty("name", (String) row[1]);
                brands.add(b);
            }

            responseObject.add("brands", brands);
            status = true;
            message = "Brands loaded";
        } catch (Exception e) {
            e.printStackTrace();
            message = "Error loading brands";
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getSizes() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> data = hibernateSession.createQuery("SELECT s.id, s.name FROM Size s", Object[].class)
                    .list();

            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (Object[] row : data) {
                JsonObject o = new JsonObject();
                o.addProperty("id", (Integer) row[0]);
                o.addProperty("name", (String) row[1]);
                arr.add(o);
            }

            responseObject.add("sizes", arr);
            status = true;
            message = "Sizes loaded";
        } catch (Exception e) {
            e.printStackTrace();
            message = "Error loading sizes";
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getColors() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> data = hibernateSession.createQuery("SELECT c.id, c.name FROM Color c", Object[].class)
                    .list();

            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (Object[] row : data) {
                JsonObject o = new JsonObject();
                o.addProperty("id", (Integer) row[0]);
                o.addProperty("name", (String) row[1]);
                arr.add(o);
            }

            responseObject.add("colors", arr);
            status = true;
            message = "Colors loaded";
        } catch (Exception e) {
            e.printStackTrace();
            message = "Error loading colors";
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String searchProducts(SearchRequestDTO request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        List<ProductDTO> productDTOs = new ArrayList<>();
        // Removed unused totalResults

        // Validation
        if (request.getPage() < 1)
            request.setPage(1);
        if (request.getPageSize() < 1)
            request.setPageSize(12);

        if (request.getMinPrice() != null && request.getMinPrice() < 0) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Minimum price cannot be negative");
            return AppUtil.GSON.toJson(responseObject);
        }
        if (request.getMaxPrice() != null && request.getMaxPrice() < 0) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Maximum price cannot be negative");
            return AppUtil.GSON.toJson(responseObject);
        }
        if (request.getMinPrice() != null && request.getMaxPrice() != null
                && request.getMinPrice() > request.getMaxPrice()) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Minimum price cannot be greater than maximum price");
            return AppUtil.GSON.toJson(responseObject);
        }

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

            // Build HQL dynamically
            StringBuilder hql = new StringBuilder("SELECT DISTINCT p FROM Product p ");

            List<String> conditions = new ArrayList<>();
            conditions.add("p.status.type = 'Active'");

            // Text Search
            if (request.getQuery() != null && !request.getQuery().isBlank()) {
                conditions.add("(LOWER(p.name) LIKE LOWER(:query) OR LOWER(p.description) LIKE LOWER(:query))");
            }

            // Brands
            if (request.getBrandIds() != null && !request.getBrandIds().isEmpty()) {
                conditions.add("p.brand.id IN (:brandIds)");
            }

            // Stock Filters
            boolean hasStockFilters = (request.getSizeIds() != null && !request.getSizeIds().isEmpty()) ||
                    (request.getColorIds() != null && !request.getColorIds().isEmpty()) ||
                    request.getMinPrice() != null ||
                    request.getMaxPrice() != null;

            // Subquery for stock check
            StringBuilder stockSubquery = new StringBuilder(
                    "EXISTS (SELECT 1 FROM Stock s WHERE s.product = p AND s.status.type = 'Active' AND s.qty > 0");

            if (request.getSizeIds() != null && !request.getSizeIds().isEmpty()) {
                stockSubquery.append(" AND s.size.id IN (:sizeIds)");
            }
            if (request.getColorIds() != null && !request.getColorIds().isEmpty()) {
                stockSubquery.append(" AND s.color.id IN (:colorIds)");
            }
            if (request.getMinPrice() != null) {
                stockSubquery.append(" AND s.sellingPrice >= :minPrice");
            }
            if (request.getMaxPrice() != null) {
                stockSubquery.append(" AND s.sellingPrice <= :maxPrice");
            }
            stockSubquery.append(")");

            if (hasStockFilters) {
                conditions.add(stockSubquery.toString());
            } else {
                // Even without filters, ensure at least one active stock exists
                conditions.add(
                        "(SELECT COUNT(s) FROM Stock s WHERE s.product = p AND s.status.type = 'Active' AND s.qty > 0) > 0");
            }

            // Assemble Where Clause
            hql.append(" WHERE ");
            hql.append(String.join(" AND ", conditions));

            // Sorting
            if (request.getSort() != null) {
                switch (request.getSort()) {
                    case "PRICE_ASC":
                        hql.append(
                                " ORDER BY (SELECT MIN(s.sellingPrice) FROM Stock s WHERE s.product = p AND s.status.type = 'Active' AND s.qty > 0) ASC");
                        break;
                    case "PRICE_DESC":
                        hql.append(
                                " ORDER BY (SELECT MIN(s.sellingPrice) FROM Stock s WHERE s.product = p AND s.status.type = 'Active' AND s.qty > 0) DESC");
                        break;
                    case "POPULARITY":
                        hql.append(" ORDER BY p.id ASC");
                        break;
                    case "NEWEST":
                    default:
                        hql.append(" ORDER BY p.id DESC");
                        break;
                }
            } else {
                hql.append(" ORDER BY p.id DESC");
            }

            // Execute Query
            org.hibernate.query.Query<com.zentora.nike_x.entity.Product> query = hibernateSession
                    .createQuery(hql.toString(), com.zentora.nike_x.entity.Product.class);

            // Set Parameters
            if (request.getQuery() != null && !request.getQuery().isBlank()) {
                query.setParameter("query", "%" + request.getQuery().trim() + "%");
            }
            if (request.getBrandIds() != null && !request.getBrandIds().isEmpty()) {
                query.setParameterList("brandIds", request.getBrandIds());
            }
            if (request.getSizeIds() != null && !request.getSizeIds().isEmpty()) {
                query.setParameterList("sizeIds", request.getSizeIds());
            }
            if (request.getColorIds() != null && !request.getColorIds().isEmpty()) {
                query.setParameterList("colorIds", request.getColorIds());
            }
            if (request.getMinPrice() != null) {
                query.setParameter("minPrice", request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                query.setParameter("maxPrice", request.getMaxPrice());
            }

            // Pagination
            query.setFirstResult((request.getPage() - 1) * request.getPageSize());
            query.setMaxResults(request.getPageSize());

            List<com.zentora.nike_x.entity.Product> products = query.list();

            // Convert to DTO
            for (com.zentora.nike_x.entity.Product product : products) {
                ProductDTO dto = new ProductDTO();
                dto.setId(product.getId());
                dto.setName(product.getName());
                dto.setBrandName(product.getBrand().getName());
                dto.setModelName(product.getModel().getName());
                dto.setGender(product.getGender().getName());

                // Get First Image
                String hqlImage = "SELECT p.path FROM ProductImage p WHERE p.product.id = :pid";
                List<String> images = hibernateSession.createQuery(hqlImage, String.class)
                        .setParameter("pid", product.getId())
                        .setMaxResults(1)
                        .list();
                if (!images.isEmpty()) {
                    dto.setImagePath(images.get(0));
                }

                // Get Lowest Active Price
                String hqlPrice = "SELECT MIN(s.sellingPrice) FROM Stock s WHERE s.product.id = :pid AND s.status.type = 'Active' AND s.qty > 0";
                Double minPrice = hibernateSession.createQuery(hqlPrice, Double.class)
                        .setParameter("pid", product.getId())
                        .uniqueResult();
                dto.setPrice(minPrice != null ? minPrice : 0.0);

                productDTOs.add(dto);
            }

            status = true;
            message = "Success";

            // Get total count for pagination (Approximate)
            // Pagination handled by simple list return for now
            // totalResults logic removed as it was unused and causing warnings

        } catch (Exception e) {
            e.printStackTrace();
            message = "Error searching products";
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("products", AppUtil.GSON.toJsonTree(productDTOs));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getProductById(String id) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            if (id == null || id.isEmpty()) {
                message = "Product ID is required";
            } else {
                // Use HQL to fetch product eagerly with status to ensure no lazy loading issues
                // and safe checking
                String hql = "SELECT p FROM Product p JOIN FETCH p.status WHERE p.id = :id";
                Product product = hibernateSession.createQuery(hql, Product.class)
                        .setParameter("id", Integer.parseInt(id))
                        .uniqueResult();

                if (product != null) {
                    System.out.println(
                            "Product Found: " + product.getName() + " | Status: " + product.getStatus().getType());
                }

                if (product != null && "Active".equalsIgnoreCase(product.getStatus().getType())) {

                    // Main Product Data
                    ProductDTO productDTO = new ProductDTO();
                    productDTO.setId(product.getId());
                    productDTO.setName(product.getName());
                    productDTO.setDescription(product.getDescription());

                    // Get Price
                    Double minPrice = (Double) hibernateSession.createQuery(
                            "SELECT MIN(s.sellingPrice) FROM Stock s WHERE s.product = :product AND s.status.type = 'Active' AND s.qty > 0")
                            .setParameter("product", product)
                            .uniqueResult();
                    productDTO.setPrice(minPrice != null ? minPrice : 0.0);

                    productDTO.setBrandName(product.getBrand().getName());
                    productDTO.setBrandId(product.getBrand().getId());
                    productDTO.setModelName(product.getModel().getName());
                    productDTO.setGender(product.getGender().getName());

                    // Fetch Images with Color info
                    List<ProductImage> productImages = hibernateSession.createQuery(
                            "SELECT pi FROM ProductImage pi LEFT JOIN FETCH pi.stock s LEFT JOIN FETCH s.color WHERE pi.product = :product",
                            ProductImage.class)
                            .setParameter("product", product)
                            .list();

                    com.google.gson.JsonArray images = new com.google.gson.JsonArray();
                    if (!productImages.isEmpty()) {
                        productDTO.setImagePath(productImages.get(0).getPath()); // Main image fallback
                        for (ProductImage pi : productImages) {
                            JsonObject imgObj = new JsonObject();
                            imgObj.addProperty("path", pi.getPath());
                            if (pi.getStock() != null && pi.getStock().getColor() != null) {
                                imgObj.addProperty("colorId", pi.getStock().getColor().getId());
                            } else {
                                imgObj.add("colorId", null);
                            }
                            images.add(imgObj);
                        }
                    }

                    // Fetch Available Sizes & Colors from Stock
                    // Only active stocks
                    // Fetch Available Sizes & Colors from Stock with Price
                    // Only active stocks
                    // Fetch Available Sizes & Colors from Stock with Price & StockID
                    // Only active stocks
                    List<Object[]> stockData = hibernateSession.createQuery(
                            "SELECT s.size.id, s.size.name, s.color.id, s.color.name, MIN(s.sellingPrice), MIN(s.id) FROM Stock s "
                                    +
                                    "WHERE s.product = :product AND s.status.type = 'Active' AND s.qty > 0 " +
                                    "GROUP BY s.size.id, s.size.name, s.color.id, s.color.name",
                            Object[].class)
                            .setParameter("product", product)
                            .list();

                    com.google.gson.JsonArray sizes = new com.google.gson.JsonArray();
                    com.google.gson.JsonArray colors = new com.google.gson.JsonArray();

                    List<Integer> addedSizeIds = new ArrayList<>();
                    List<Integer> addedColorIds = new ArrayList<>();

                    com.google.gson.JsonArray combinations = new com.google.gson.JsonArray(); // New

                    for (Object[] row : stockData) {
                        Integer sizeId = (Integer) row[0];
                        String sizeName = (String) row[1];
                        Integer colorId = (Integer) row[2];
                        String colorName = (String) row[3];
                        Double price = (Double) row[4];
                        Integer stockId = (Integer) row[5];

                        if (!addedSizeIds.contains(sizeId)) {
                            JsonObject s = new JsonObject();
                            s.addProperty("id", sizeId);
                            s.addProperty("name", sizeName);
                            sizes.add(s);
                            addedSizeIds.add(sizeId);
                        }

                        if (!addedColorIds.contains(colorId)) {
                            JsonObject c = new JsonObject();
                            c.addProperty("id", colorId);
                            c.addProperty("name", colorName);
                            colors.add(c);
                            addedColorIds.add(colorId);
                        }

                        // Add combination
                        JsonObject combo = new JsonObject();
                        combo.addProperty("sizeId", sizeId);
                        combo.addProperty("colorId", colorId);
                        combo.addProperty("price", price);
                        combo.addProperty("stockId", stockId);
                        combinations.add(combo);
                    }

                    responseObject.add("stockCombinations", combinations); // Add to response

                    responseObject.add("product", AppUtil.GSON.toJsonTree(productDTO));
                    responseObject.add("images", AppUtil.GSON.toJsonTree(images));
                    responseObject.add("sizes", sizes);
                    responseObject.add("colors", colors);

                    status = true;
                    message = "Product found";
                } else {
                    message = "Product not found or inactive";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            message = "Error loading product";
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getRelatedProducts(String brandIdStr, String currentProductIdStr) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            int brandId = Integer.parseInt(brandIdStr);
            int currentProductId = Integer.parseInt(currentProductIdStr);

            // 1. Get Current Product to find Gender
            Product currentProduct = hibernateSession.get(Product.class, currentProductId);
            if (currentProduct == null) {
                return AppUtil.GSON.toJson(responseObject);
            }
            int genderId = currentProduct.getGender().getId();

            List<Product> relatedProducts = new ArrayList<>();

            // 2. Strategy A: Same Brand + Same Gender, Sorted by Sales
            String hqlA = "SELECT p FROM Product p " +
                    "WHERE p.brand.id = :brandId " +
                    "AND p.gender.id = :genderId " +
                    "AND p.id != :currentId " +
                    "AND p.status.type = 'Active' " +
                    "ORDER BY (SELECT COALESCE(SUM(ii.qty), 0) FROM InvoiceItem ii WHERE ii.stock.product = p) DESC";

            List<Product> listA = hibernateSession.createQuery(hqlA, Product.class)
                    .setParameter("brandId", brandId)
                    .setParameter("genderId", genderId)
                    .setParameter("currentId", currentProductId)
                    .setMaxResults(3)
                    .list();

            relatedProducts.addAll(listA);

            // 3. Strategy B: Fallback (Same Gender, Any Brand), Sorted by Sales
            if (relatedProducts.size() < 3) {
                int limit = 3 - relatedProducts.size();
                String hqlB = "SELECT p FROM Product p " +
                        "WHERE p.gender.id = :genderId " +
                        "AND p.brand.id != :brandId " + // Exclude already searched brand if we want strictly
                                                        // complementary or just avoid dupes (though ID check covers
                                                        // dupes, this speeds up or diversifies)
                        "AND p.id != :currentId " +
                        "AND p.status.type = 'Active' " +
                        "ORDER BY (SELECT COALESCE(SUM(ii.qty), 0) FROM InvoiceItem ii WHERE ii.stock.product = p) DESC";

                List<Product> listB = hibernateSession.createQuery(hqlB, Product.class)
                        .setParameter("genderId", genderId)
                        .setParameter("brandId", brandId)
                        .setParameter("currentId", currentProductId)
                        .setMaxResults(limit)
                        .list();

                relatedProducts.addAll(listB);
            }

            // Convert to DTOs
            List<ProductDTO> dtos = new ArrayList<>();
            for (Product p : relatedProducts) {
                ProductDTO dto = new ProductDTO();
                dto.setId(p.getId());
                dto.setName(p.getName());

                // Get Price
                Double minPrice = (Double) hibernateSession.createQuery(
                        "SELECT MIN(s.sellingPrice) FROM Stock s WHERE s.product = :p AND s.status.type = 'Active' AND s.qty > 0")
                        .setParameter("p", p)
                        .uniqueResult();
                dto.setPrice(minPrice != null ? minPrice : 0.0);

                dto.setBrandName(p.getBrand().getName());
                dto.setModelName(p.getModel().getName());
                dto.setGender(p.getGender().getName());

                // Get main image
                List<ProductImage> imgs = hibernateSession.createQuery(
                        "SELECT pi FROM ProductImage pi WHERE pi.product = :p", ProductImage.class)
                        .setParameter("p", p)
                        .setMaxResults(1)
                        .list();
                if (!imgs.isEmpty())
                    dto.setImagePath(imgs.get(0).getPath());

                dtos.add(dto);
            }

            responseObject.add("products", AppUtil.GSON.toJsonTree(dtos));
            status = true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        return AppUtil.GSON.toJson(responseObject);
    }

    public com.zentora.nike_x.dto.CartItemDTO getProductByStockId(int stockId) {
        com.zentora.nike_x.dto.CartItemDTO itemDTO = null;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            com.zentora.nike_x.entity.Stock stock = hibernateSession.get(com.zentora.nike_x.entity.Stock.class,
                    stockId);

            if (stock != null && "Active".equalsIgnoreCase(stock.getStatus().getType())) {
                itemDTO = new com.zentora.nike_x.dto.CartItemDTO();
                itemDTO.setStockId(stock.getId()); // Use setStockId instead of setStock if flattened

                // Manually map fields to match CartItemDTO structure
                itemDTO.setProductId(stock.getProduct().getId());
                itemDTO.setProductName(stock.getProduct().getName());
                itemDTO.setPrice(stock.getSellingPrice());
                itemDTO.setSizeId(stock.getSize().getId());
                itemDTO.setSizeName(stock.getSize().getName());
                itemDTO.setColorId(stock.getColor().getId());
                itemDTO.setColorName(stock.getColor().getName());
                // itemDTO.setQty(1); // Qty is determined by user input, handled by caller or
                // default 1

                // Get Image
                String hqlImage = "SELECT p.path FROM ProductImage p WHERE p.product=:prod";
                List<String> images = hibernateSession.createQuery(hqlImage, String.class)
                        .setParameter("prod", stock.getProduct())
                        .setMaxResults(1)
                        .list();
                if (!images.isEmpty())
                    itemDTO.setImagePath(images.get(0));

                // Flattened props if your DTO has them, otherwise nested?
                // Based on previous logs, CartItemDTO seems flat-ish or checkout.js expects
                // flat
                // checking checkout.js earlier: item.productName, item.price.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemDTO;
    }

    public String getSuggestions(String query) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        List<JsonObject> suggestions = new ArrayList<>();

        if (query == null || query.isBlank()) {
            responseObject.addProperty("status", false);
            responseObject.add("suggestions", AppUtil.GSON.toJsonTree(suggestions));
            return AppUtil.GSON.toJson(responseObject);
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Search across product name, brand name, and model name
            String hql = "SELECT p.id, p.name, p.brand.name, p.model.name FROM Product p " +
                    "WHERE p.status.type = 'Active' AND (" +
                    "LOWER(p.name) LIKE LOWER(:query) OR " +
                    "LOWER(p.brand.name) LIKE LOWER(:query) OR " +
                    "LOWER(p.model.name) LIKE LOWER(:query) OR " +
                    "LOWER(CONCAT(p.brand.name, ' ', p.name)) LIKE LOWER(:query)" +
                    ") ORDER BY p.name ASC";

            List<Object[]> results = session.createQuery(hql, Object[].class)
                    .setParameter("query", "%" + query.trim() + "%")
                    .setMaxResults(6)
                    .list();

            for (Object[] row : results) {
                JsonObject s = new JsonObject();
                s.addProperty("id", (Integer) row[0]);
                s.addProperty("name", (String) row[1]);
                s.addProperty("brand", (String) row[2]);
                s.addProperty("model", (String) row[3]);
                suggestions.add(s);
            }
            status = true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.add("suggestions", AppUtil.GSON.toJsonTree(suggestions));
        return AppUtil.GSON.toJson(responseObject);
    }
}
