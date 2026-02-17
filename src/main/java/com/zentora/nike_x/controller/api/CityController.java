package com.zentora.nike_x.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zentora.nike_x.entity.City;
import com.zentora.nike_x.util.HibernateUtil;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hibernate.Session;

import java.util.List;

@Path("/cities")
public class CityController {

    private static final Gson GSON = new Gson();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCities() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "Error fetching cities";

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<City> cities = session.createQuery("FROM City ORDER BY name ASC", City.class).getResultList();

            JsonArray cityArray = new JsonArray();
            for (City city : cities) {
                JsonObject cityObj = new JsonObject();
                cityObj.addProperty("id", city.getId());
                cityObj.addProperty("name", city.getName());
                cityArray.add(cityObj);
            }

            responseObject.add("cities", cityArray);
            status = true;
            message = "Success";
        } catch (Exception e) {
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return Response.ok().entity(GSON.toJson(responseObject)).build();
    }
}
