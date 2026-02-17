package com.zentora.nike_x.controller.api;

import com.zentora.nike_x.dto.SearchRequestDTO;
import com.zentora.nike_x.service.ProductService;
import com.zentora.nike_x.util.AppUtil;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/products")
public class ProductController {

    @GET
    @Path("/new-drops")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNewDrops() {
        ProductService productService = new ProductService();
        String response = productService.getHomeProducts();
        return Response.ok(response).build();
    }

    @POST
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @jakarta.ws.rs.Consumes(MediaType.APPLICATION_JSON)
    public Response searchProducts(String jsonBody) {
        SearchRequestDTO request = AppUtil.GSON.fromJson(jsonBody, SearchRequestDTO.class);
        ProductService productService = new ProductService();
        String response = productService.searchProducts(request);
        return Response.ok(response).build();
    }

    @GET
    @Path("/brands")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBrands() {
        ProductService productService = new ProductService();
        String response = productService.getBrands();
        return Response.ok(response).build();
    }

    @GET
    @Path("/sizes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSizes() {
        ProductService productService = new ProductService();
        String response = productService.getSizes();
        return Response.ok(response).build();
    }

    @GET
    @Path("/colors")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getColors() {
        ProductService productService = new ProductService();
        String response = productService.getColors();
        return Response.ok(response).build();
    }

    @GET
    @Path("/details")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductDetails(@jakarta.ws.rs.QueryParam("id") String id) {
        ProductService productService = new ProductService();
        String response = productService.getProductById(id);
        return Response.ok(response).build();
    }

    @GET
    @Path("/related")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRelatedProducts(@jakarta.ws.rs.QueryParam("brand_id") String brandId,
            @jakarta.ws.rs.QueryParam("product_id") String productId) {
        ProductService productService = new ProductService();
        String response = productService.getRelatedProducts(brandId, productId);
        return Response.ok(response).build();
    }

    @GET
    @Path("/reviews")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReviews(@jakarta.ws.rs.QueryParam("product_id") int productId) {
        com.zentora.nike_x.service.ReviewService reviewService = new com.zentora.nike_x.service.ReviewService();
        String response = reviewService.getReviews(productId);
        return Response.ok(response).build();
    }

    @POST
    @Path("/reviews")
    @Produces(MediaType.APPLICATION_JSON)
    @jakarta.ws.rs.Consumes(MediaType.APPLICATION_JSON)
    public Response addReview(String jsonBody,
            @jakarta.ws.rs.core.Context jakarta.servlet.http.HttpServletRequest req) {
        com.zentora.nike_x.dto.UserDTO user = (com.zentora.nike_x.dto.UserDTO) req.getSession().getAttribute("user");
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"status\":false, \"message\":\"Please login\"}").build();
        }

        com.zentora.nike_x.dto.ReviewDTO reviewDTO = AppUtil.GSON.fromJson(jsonBody,
                com.zentora.nike_x.dto.ReviewDTO.class);
        com.zentora.nike_x.service.ReviewService reviewService = new com.zentora.nike_x.service.ReviewService();

        String appPath = req.getServletContext().getRealPath("");
        String response = reviewService.addReview(reviewDTO, user.getId(), appPath);

        return Response.ok(response).build();
    }

    @GET
    @Path("/reviews/check-eligibility")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkEligibility(@jakarta.ws.rs.QueryParam("product_id") int productId,
            @jakarta.ws.rs.core.Context jakarta.servlet.http.HttpServletRequest req) {
        com.zentora.nike_x.dto.UserDTO user = (com.zentora.nike_x.dto.UserDTO) req.getSession().getAttribute("user");
        if (user == null) {
            return Response.ok("{\"status\":false}").build();
        }

        com.zentora.nike_x.service.ReviewService reviewService = new com.zentora.nike_x.service.ReviewService();
        String response = reviewService.checkEligibility(user.getId(), productId);
        return Response.ok(response).build();
    }

    @GET
    @Path("/stock")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStockDetails(@jakarta.ws.rs.QueryParam("id") int stockId) {
        ProductService productService = new ProductService();
        com.zentora.nike_x.dto.CartItemDTO dto = productService.getProductByStockId(stockId);

        if (dto != null) {
            return Response.ok(AppUtil.GSON.toJson(dto)).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"message\":\"Stock not found\"}").build();
        }
    }

    @GET
    @Path("/suggest")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSuggestions(@jakarta.ws.rs.QueryParam("query") String query) {
        ProductService productService = new ProductService();
        String response = productService.getSuggestions(query);
        return Response.ok(response).build();
    }
}
