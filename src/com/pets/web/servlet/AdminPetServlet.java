package com.pets.web.servlet;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pets.domain.Category;
import com.pets.domain.Pet;
import com.pets.pager.PageBean;
import com.pets.service.CategoryService;
import com.pets.service.PetService;
import com.pets.servlet.BaseServlet;
import com.pets.utils.CommonUtils;

public class AdminPetServlet extends BaseServlet {

	private PetService petService = new PetService();
	private CategoryService  categoryService = new CategoryService();
	/**
	 * 删除
	 * @param req
	 * @param resp
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public String delete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String bid = req.getParameter("bid");
		
		/*
		 * 删除图片
		 */
		Pet pet = petService.load(bid);
		String savepath = this.getServletContext().getRealPath("/");//获取真实的路径
		new File(savepath, pet.getImage_w()).delete();//删除文件
		new File(savepath, pet.getImage_b()).delete();//删除文件
		
		petService.delete(bid);//删除数据库的记录
		
		req.setAttribute("msg", "删除成功！");
		return "f:/adminjsps/msg.jsp";
	}
	
	/**
	 * 修改
	 * @param req
	 * @param resp
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public String edit(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		/*
		 * 1. 把表单数据封装到Pet对象中
		 * 2. 封装cid到Category中
		 * 3. 把Category赋给Pet
		 * 4. 调用service完成工作
		 * 5. 保存成功信息，转发到msg.jsp
		 */
		Map map = req.getParameterMap();
		Pet pet = CommonUtils.toBean(map, Pet.class);
		Category category = CommonUtils.toBean(map, Category.class);
		pet.setCategory(category);
		
		petService.edit(pet);
		req.setAttribute("msg", "修改成功！");
		return "f:/adminjsps/msg.jsp";
	}
	
	/**
	 * 加载
	 * @param req
	 * @param resp
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public String load(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		/*
		 * 1. 获取bid，得到Pet对象，保存之
		 */
		String bid = req.getParameter("bid");
		Pet pet = petService.load(bid);
		req.setAttribute("pet", pet);
		
		/*
		 * 2. 获取所有一级分类，保存之
		 */
		req.setAttribute("parents", categoryService.findParents());
		/*
		 * 3. 获取当前图书所属的一级分类下所有2级分类
		 */
		String pid = pet.getCategory().getParent().getCid();
		req.setAttribute("children", categoryService.findChildren(pid));
		
		/*
		 * 4. 转发到desc.jsp显示
		 */
		return "f:/adminjsps/admin/pet/desc.jsp";
	}
	
	/**
	 * 添加：第一步
	 * @param req
	 * @param resp
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public String addPre(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		/*
		 * 1. 获取所有一级分类，保存之
		 * 2. 转发到add.jsp，该页面会在下拉列表中显示所有一级分类
		 */
		List<Category> parents = categoryService.findParents();
		req.setAttribute("parents", parents);
		return "f:/adminjsps/admin/pet/add.jsp";
	}
	
	public String ajaxFindChildren(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		/*
		 * 1. 获取pid
		 * 2. 通过pid查询出所有2级分类
		 * 3. 把List<Category>转换成json，输出给客户端
		 */
		String pid = req.getParameter("pid");
		List<Category> children = categoryService.findChildren(pid);
		String json = toJson(children);
		resp.getWriter().print(json);
		return null;
	}
	
	// {"cid":"fdsafdsa", "cname":"fdsafdas"}
	private String toJson(Category category) {
		StringBuilder sb = new StringBuilder("{");
		sb.append("\"cid\"").append(":").append("\"").append(category.getCid()).append("\"");
		sb.append(",");
		sb.append("\"cname\"").append(":").append("\"").append(category.getCname()).append("\"");
		sb.append("}");
		return sb.toString();
	}
	
	// [{"cid":"fdsafdsa", "cname":"fdsafdas"}, {"cid":"fdsafdsa", "cname":"fdsafdas"}]
	private String toJson(List<Category> categoryList) {
		StringBuilder sb = new StringBuilder("[");
		for(int i = 0; i < categoryList.size(); i++) {
			sb.append(toJson(categoryList.get(i)));
			if(i < categoryList.size() - 1) {
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}
	
	/**
	 * 显示所有分类
	 * @param req
	 * @param resp
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public String findCategoryAll(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		/*
		 * 1. 通过service得到所有的分类
		 * 2. 保存到request中，转发到left.jsp
		 */
		List<Category> parents = categoryService.findAll();
		req.setAttribute("parents", parents);
		return "f:/adminjsps/admin/pet/left.jsp";
	}
	
	
	
	
	
	
	
	/**
	 * 获取当前页码
	 * @param req
	 * @return
	 */
	private int getPc(HttpServletRequest req) {
		int pc = 1;
		String param = req.getParameter("pc");
		if(param != null && !param.trim().isEmpty()) {
			try {
				pc = Integer.parseInt(param);
			} catch(RuntimeException e) {}
		}
		return pc;
	}
	
	/**
	 * 截取url，页面中的分页导航中需要使用它做为超链接的目标！
	 * @param req
	 * @return
	 */

	private String getUrl(HttpServletRequest req) {
		String url = req.getRequestURI() + "?" + req.getQueryString();
		/*
		 * 如果url中存在pc参数，截取掉，如果不存在那就不用截取。
		 */
		int index = url.lastIndexOf("&pc=");
		if(index != -1) {
			url = url.substring(0, index);
		}
		return url;
	}
	
	/**
	 * 按分类查
	 * @param req
	 * @param resp
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public String findByCategory(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		/*
		 * 1. 得到pc：如果页面传递，使用页面的，如果没传，pc=1
		 */
		int pc = getPc(req);
		/*
		 * 2. 得到url：...
		 */
		String url = getUrl(req);
		/*
		 * 3. 获取查询条件，本方法就是cid，即分类的id
		 */
		String cid = req.getParameter("cid");
		/*
		 * 4. 使用pc和cid调用service#findByCategory得到PageBean
		 */
		PageBean<Pet> pb = petService.findByCategory(cid, pc);
		/*
		 * 5. 给PageBean设置url，保存PageBean
		 */
		pb.setUrl(url);
		req.setAttribute("pb", pb);
		return "f:/adminjsps/admin/pet/list.jsp";
	}
	
	/**
	 * 按名称查询
	 * @param req
	 * @param resp
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public String findByBname(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		/*
		 * 1. 得到pc：如果页面传递，使用页面的，如果没传，pc=1
		 */
		int pc = getPc(req);
		/*
		 * 2. 得到url：...
		 */
		String url = getUrl(req);
		/*
		 * 3. 获取查询条件，本方法就是cid，即分类的id
		 */
		String bname = req.getParameter("bname");
		/*
		 * 4. 使用pc和cid调用service#findByCategory得到PageBean
		 */
		PageBean<Pet> pb = petService.findByBname(bname, pc);
		/*
		 * 5. 给PageBean设置url，保存PageBean，
		 */
		pb.setUrl(url);
		req.setAttribute("pb", pb);
		return "f:/adminjsps/admin/pet/list.jsp";
	}
	
//	/**
//	 * 多条件组合查询
//	 * @param req
//	 * @param resp
//	 * @return
//	 * @throws ServletException
//	 * @throws IOException
//	 */
//	public String findByCombination(HttpServletRequest req, HttpServletResponse resp)
//			throws ServletException, IOException {
//		/*
//		 * 1. 得到pc：如果页面传递，使用页面的，如果没传，pc=1
//		 */
//		int pc = getPc(req);
//		/*
//		 * 2. 得到url：...
//		 */
//		String url = getUrl(req);
//		/*
//		 * 3. 获取查询条件，本方法就是cid，即分类的id
//		 */
//		Pet criteria = CommonUtils.toBean(req.getParameterMap(), Pet.class);
//		/*
//		 * 4. 使用pc和cid调用service#findByCategory得到PageBean
//		 */
//		PageBean<Pet> pb = petService.findByCombination(criteria, pc);
//		/*
//		 * 5. 给PageBean设置url，保存PageBean，
//		 */
//		pb.setUrl(url);
//		req.setAttribute("pb", pb);
//		return "f:/adminjsps/admin/pet/list.jsp";
//	}
}
