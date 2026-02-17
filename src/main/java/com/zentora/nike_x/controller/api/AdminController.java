package com.zentora.nike_x.controller.api;

import com.zentora.nike_x.dto.GrnDTO;
import com.zentora.nike_x.dto.GrnItemDTO;
import com.zentora.nike_x.dto.GrnItemUpdateDTO;
import com.zentora.nike_x.dto.ProductDTO;
import com.zentora.nike_x.dto.SupplierDTO;
import com.zentora.nike_x.dto.UserDTO;
import com.zentora.nike_x.service.AdminService;
import com.zentora.nike_x.util.AppUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.util.List;

@Path("/admin")
public class AdminController {

    @Path("/send-code")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendCode(String jsonData) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new AdminService().sendCode(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/sign-in")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response signIn(String jsonData, @Context HttpServletRequest request) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new AdminService().signIn(userDTO, request);
        return Response.ok().entity(responseJson).build();
    }

    @GET
    @Path("/auth-check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkAuth(@Context HttpServletRequest request) {
        AdminService adminService = new AdminService();
        String response = adminService.checkAuthStatus(request);
        return Response.ok(response).build();
    }

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStats() {
        AdminService adminService = new AdminService();
        String response = adminService.getDashboardStats();
        return Response.ok(response).build();
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers() {
        AdminService adminService = new AdminService();
        String response = adminService.getUsers();
        return Response.ok(response).build();
    }

    @GET
    @Path("/orders")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrders(@QueryParam("page") int page) {
        if (page < 1)
            page = 1;
        // Default limit 10 for dashboard/list
        int limit = 10;
        AdminService adminService = new AdminService();
        String response = adminService.getAllOrders(page, limit);
        return Response.ok(response).build();
    }

    @GET
    @Path("/order/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrderDetails(@jakarta.ws.rs.PathParam("id") int id) {
        AdminService adminService = new AdminService();
        String response = adminService.getOrderDetails(id);
        return Response.ok(response).build();
    }

    @POST
    @Path("/order/update-status")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrderStatus(@FormParam("orderId") int orderId, @FormParam("status") String status) {
        AdminService adminService = new AdminService();
        String response = adminService.updateOrderStatus(orderId, status);
        return Response.ok(response).build();
    }

    @POST
    @Path("/user/update-status")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserStatus(@FormParam("userId") int userId, @FormParam("action") String action) {
        AdminService adminService = new AdminService();
        String response = adminService.updateUserStatus(userId, action);
        return Response.ok(response).build();
    }

    @POST
    @Path("/product/add")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addProduct(@FormDataParam("product") String productJson, @Context HttpServletRequest request) {
        ProductDTO productDTO = AppUtil.GSON.fromJson(productJson, ProductDTO.class);
        AdminService adminService = new AdminService();
        String response = adminService.addProduct(productDTO, request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/product/{id}/upload-images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadImages(@jakarta.ws.rs.PathParam("id") int productId,
            @FormDataParam("images[]") List<FormDataBodyPart> bodyParts,
            @Context HttpServletRequest request) {
        AdminService adminService = new AdminService();
        String response = adminService.uploadImages(productId, bodyParts, request);
        return Response.ok(response).build();
    }

    @jakarta.ws.rs.GET
    @Path("/brand/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBrands() {
        AdminService adminService = new AdminService();
        String response = adminService.getBrands();
        return Response.ok(response).build();
    }

    @GET
    @Path("/color/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getColors() {
        AdminService adminService = new AdminService();
        String response = adminService.getColors();
        return Response.ok(response).build();
    }

    @GET
    @Path("/size/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSizes() {
        AdminService adminService = new AdminService();
        String response = adminService.getSizes();
        return Response.ok(response).build();
    }

    @POST
    @Path("/color/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addColor(@FormParam("name") String name) {
        AdminService adminService = new AdminService();
        String response = adminService.saveColor(name);
        return Response.ok(response).build();
    }

    @POST
    @Path("/size/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSize(@FormParam("name") String name) {
        AdminService adminService = new AdminService();
        String response = adminService.saveSize(name);
        return Response.ok(response).build();
    }

    @GET
    @Path("/product/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProducts(@QueryParam("page") int page) {
        if (page < 1)
            page = 1;
        AdminService adminService = new AdminService();
        String response = adminService.getProducts(page);
        return Response.ok(response).build();
    }

    @GET
    @Path("/product/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductDetails(@jakarta.ws.rs.PathParam("id") int id) {
        AdminService adminService = new AdminService();
        String response = adminService.getProductDetails(id);
        return Response.ok(response).build();
    }

    @POST
    @Path("/product/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProduct(String jsonData) {
        ProductDTO productDTO = AppUtil.GSON.fromJson(jsonData, ProductDTO.class);
        AdminService adminService = new AdminService();
        String response = adminService.updateProduct(productDTO);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/product/image/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeProductImage(@jakarta.ws.rs.PathParam("id") int id, @Context HttpServletRequest request) {
        AdminService adminService = new AdminService();
        String response = adminService.removeProductImage(id, request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/grn/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveGrn(String grnDTOJson) {
        GrnDTO grnDTO = AppUtil.GSON.fromJson(grnDTOJson, GrnDTO.class);
        AdminService adminService = new AdminService();
        String response = adminService.saveGrn(grnDTO);
        return Response.ok(response).build();
    }

    @POST
    @Path("/grn/validate-item")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateGrnItem(String itemDTOJson) {
        GrnItemDTO itemDTO = AppUtil.GSON.fromJson(itemDTOJson, GrnItemDTO.class);
        AdminService adminService = new AdminService();
        String response = adminService.validateGrnItem(itemDTO);
        return Response.ok(response).build();
    }

    @GET
    @Path("/grn/recent-items")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecentGrnItems(@QueryParam("page") int page) {
        if (page == 0)
            page = 1;
        AdminService adminService = new AdminService();
        String response = adminService.getGrnItems(page);
        return Response.ok(response).build();
    }

    @GET
    @Path("/grn/list") // New Invoice List
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGrnList(@QueryParam("page") int page) {
        if (page == 0)
            page = 1;
        AdminService adminService = new AdminService();
        String response = adminService.getGrnList(page);
        return Response.ok(response).build();
    }

    @GET
    @Path("/grn/details/{id}") // Full Invoice Details
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGrnDetailsFull(@PathParam("id") int id) {
        AdminService adminService = new AdminService();
        String response = adminService.getGrnDetailsFull(id);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/grn/delete/{id}") // Delete Entire GRN
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGrn(@PathParam("id") int id) {
        AdminService adminService = new AdminService();
        String response = adminService.deleteGrn(id);
        return Response.ok(response).build();
    }

    @POST
    @Path("/grn/item/add") // Add item to GRN
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addGrnItem(String json) {
        GrnItemUpdateDTO dto = AppUtil.GSON.fromJson(json, GrnItemUpdateDTO.class);
        AdminService adminService = new AdminService();
        String response = adminService.addGrnItem(dto);
        return Response.ok(response).build();
    }

    @GET
    @Path("/supplier/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSuppliers() {
        AdminService adminService = new AdminService();
        String response = adminService.getSuppliers();
        return Response.ok(response).build();
    }

    @GET
    @Path("/grn/item-details/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGrnItemDetails(@PathParam("id") int id) {
        AdminService adminService = new AdminService();
        String response = adminService.getGrnItemDetails(id);
        return Response.ok(response).build();
    }

    @POST
    @Path("/grn/item/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateGrnItem(String json) {
        GrnItemUpdateDTO dto = AppUtil.GSON.fromJson(json, GrnItemUpdateDTO.class);
        AdminService adminService = new AdminService();
        String response = adminService.updateGrnItem(dto);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/grn/item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeGrnItem(@jakarta.ws.rs.PathParam("id") int id) {
        AdminService adminService = new AdminService();
        String response = adminService.deleteGrnItem(id);
        return Response.ok(response).build();
    }

    @POST
    @Path("/supplier/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSupplier(String jsonData) {
        SupplierDTO supplierDTO = AppUtil.GSON.fromJson(jsonData, SupplierDTO.class);
        AdminService adminService = new AdminService();
        String response = adminService.addSupplier(supplierDTO);
        return Response.ok(response).build();
    }

    @GET
    @Path("/supplier/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSupplierDetails(@jakarta.ws.rs.PathParam("id") int id) {
        AdminService adminService = new AdminService();
        String response = adminService.getSupplierDetails(id);
        return Response.ok(response).build();
    }

    @POST
    @Path("/supplier/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSupplier(String jsonData) {
        SupplierDTO supplierDTO = AppUtil.GSON.fromJson(jsonData, SupplierDTO.class);
        AdminService adminService = new AdminService();
        String response = adminService.updateSupplier(supplierDTO);
        return Response.ok(response).build();
    }

    @GET
    @Path("/product/{id}/stocks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductStocks(@jakarta.ws.rs.PathParam("id") int id) {
        AdminService adminService = new AdminService();
        String response = adminService.getProductStocks(id);
        return Response.ok(response).build();
    }

    @POST
    @Path("/stock/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateStock(String jsonData) {
        com.zentora.nike_x.dto.StockDTO stockDTO = AppUtil.GSON.fromJson(jsonData,
                com.zentora.nike_x.dto.StockDTO.class);
        AdminService adminService = new AdminService();
        String response = adminService.updateStock(stockDTO);
        return Response.ok(response).build();
    }

    @POST
    @Path("/stock/assign-image")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignStockImage(@FormParam("stockId") int stockId, @FormParam("imageId") int imageId) {
        AdminService adminService = new AdminService();
        String response = adminService.assignStockImage(stockId, imageId);
        return Response.ok(response).build();
    }
}
