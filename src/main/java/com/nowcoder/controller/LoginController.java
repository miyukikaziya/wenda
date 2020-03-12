package com.nowcoder.controller;

import com.nowcoder.async.EventModel;
import com.nowcoder.async.EventProducer;
import com.nowcoder.async.EventType;
import com.nowcoder.service.UserService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
/**
 * Created by nowcoder on 2016/7/2.
 * 负责用户登录注册
 */
@Controller
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    UserService userService;

    @Autowired
    EventProducer eventProducer;

    @RequestMapping(path = {"/reg/"}, method = RequestMethod.POST, produces="application/json; utf-8")//写入数据，post
    public String register(Model model,
                           @RequestParam("username") String username,
                           @RequestParam("password") String password,
                           @RequestParam(value = "next", required = false) String next,
                           HttpServletResponse response) {
        try {
            Map<String, String> map = userService.register(username, password);
            if (map.containsKey("ticket")) {
                Cookie cookie = new Cookie("ticket", map.get("ticket"));
                cookie.setPath("/");
                response.addCookie(cookie);
                if (StringUtils.isNotBlank(next)) {
                    return "redirect:" + next;
                }
                return "redirect:/";
            } else {//注册有问题，通过msg显示
                model.addAttribute("msg", map.get("msg"));
                return "login";
            }
        } catch (Exception e) {
            logger.error("注册异常:" + e.getMessage());
            return "login";
        }
    }
    //登录界面
    @RequestMapping(path = {"/login/"}, method = RequestMethod.POST, produces="application/json; utf-8")
    public String login(Model model,
                        @RequestParam("username") String username,
                        @RequestParam("password") String password,
                        @RequestParam(value = "next", required = false) String next,//将next参数传入，事前埋藏在html页面中
                        @RequestParam(value = "rememberme", defaultValue = "false") boolean rememberme,
                        HttpServletResponse response) {
        try {
            Map<String, String> map = userService.login(username, password);
            //把cookie放到浏览器的response中
            if (map.containsKey("ticket")) {
                Cookie cookie = new Cookie("ticket", map.get("ticket"));
                cookie.setPath("/");
                if (rememberme) {
                    cookie.setMaxAge(3600 * 24 * 5);
                }
                response.addCookie(cookie);
                try{
                    eventProducer.fireEvent(new EventModel(EventType.LOGIN)
                            .setExt("username", "zhangxue").setExt("email", "zx201353440@163.com"));
                }catch (Exception e){
                    logger.error("创建email失败！"+e.getMessage());
                }

                if (StringUtils.isNotBlank(next)) {
                    return "redirect:" + next;//返回登陆前的页面
                }
                return "redirect:/";
            } else {
                model.addAttribute("msg", map.get("msg"));
                return "login";
            }
        } catch (Exception e) {
            logger.error("登录异常:" + e.getMessage());
        }
        return "login";
    }

    @RequestMapping(path = {"/relogin"}, method = RequestMethod.GET, produces="application/json; utf-8")
    public String relogin(Model model, @RequestParam(value = "next", defaultValue = "", required = false) String next) {
        model.addAttribute("next", next);
        return "login";
    }
    //登出
    @RequestMapping(path = {"/logout"}, method = RequestMethod.GET, produces="application/json; utf-8")
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "redirect:/";
    }
}
