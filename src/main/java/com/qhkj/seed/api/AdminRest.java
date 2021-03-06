package com.qhkj.seed.api;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.qhkj.seed.auth.JwtAuthReq;
import com.qhkj.seed.auth.JwtAuthRsp;
import com.qhkj.seed.auth.Authority.AuthorityName;
import com.qhkj.seed.exceptions.ServiceException;
import com.qhkj.seed.models.BaseQueryParams;
import com.qhkj.seed.models.User;
import com.qhkj.seed.services.UserService;
import com.qhkj.seed.utils.JwtTokenHelper;
import com.qhkj.seed.utils.PATCH;
import com.qhkj.seed.utils.RestfulHelper;


@Component
@Path("/admin")
public class AdminRest extends BaseRest {

	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private JwtTokenHelper jwtTokenUtil;

	@Autowired
	private UserService userService;
	
	@GET
	@Path("/authorities")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAuthorityList() {
		AuthorityName[] list = userService.getAuthorityList();
		return Response.ok(list).build();
	}
	
	@GET
	@Path("/users/{username}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUser(@PathParam("username") String username) {
		User user = userService.getUser(username);
		if (user == null){
	        return Response.status(Response.Status.BAD_REQUEST).entity(RestfulHelper.errorResult("用户不存在!")).build();
	    }
		return Response.ok(user).build();
	}
	
	@GET
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUserList(@BeanParam BaseQueryParams bps) {
		Page<User> list = userService.getUserList(bps);
		return Response.ok(list).build();
	}

	@POST
	@Path("/token")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getToken(JwtAuthReq authRequest) {
		String username = authRequest.getUsername();
		String password = authRequest.getPassword();
		try {
			// 验证登陆账户密码
			final Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(username, password));
			
			SecurityContextHolder.getContext().setAuthentication(authentication);

			// Reload password post-security so we can generate token
			User user = userService.getUser(username);
	        if (user == null) {
	            throw new UsernameNotFoundException(String.format("用户 '%s' 不存在.", username));
	        }
			final String token = jwtTokenUtil.generateToken(username);

			// Return the token
			return Response.ok(new JwtAuthRsp(username, token, user.getMeta(), user.getAuthorities())).build();
		} catch (AuthenticationException e) {
			logger.warn(e.toString());
			return Response.status(Response.Status.BAD_REQUEST).entity(RestfulHelper.errorResult(e.getMessage()))
					.build();
		}
	}

	@POST
	@Path("/users")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response register(User user) {
		try{
			user = userService.addUser(user);
			return Response.created(null).entity(user).build();
		}catch(Exception e){
			logger.warn(e.toString());
			return Response.status(Response.Status.BAD_REQUEST).entity(RestfulHelper.errorResult(e.getMessage()))
					.build();
		}
	}

	@DELETE
    @Path("/users/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteHospital(@PathParam("id") String id) {
        try {
            userService.deleteUser(id);
            return Response.noContent().build();
        } catch (ServiceException e) {
            logger.warn(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(RestfulHelper.errorResult(e.getMessage()))
                    .build();
        }
    }
	
	@PATCH
	@Path("/users/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addAuthority(@PathParam("id") String id, User user) throws AuthenticationException {
		try {
			userService.modifyUser(id, user);
			return Response.noContent().build();
		} catch (ServiceException e) {
			logger.warn(e.getMessage());
			return Response.status(Response.Status.BAD_REQUEST).entity(RestfulHelper.errorResult(e.getMessage()))
					.build();
		}
	}
}
