package com.phoenixnap.oss.sample.server.controllers;

import java.text.DateFormat;
import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

//@RestController
public class WelcomeController {

    /**
     * 用于测试raml校验，/public/raml/api.raml中未包含对此API的定义，maven再执行raml检查时，
     * MVC实现与raml文件定义不匹配，将会导致maven执行失败
     * @param str
     * @return
     */
    @RequestMapping(value = "/welcome", method = RequestMethod.GET)
    public ResponseEntity<String> welcome(String str) {
        return new ResponseEntity<String>(
                "Welcome:" + str + "==Date:" + DateFormat.getInstance().format(new Date()),
                HttpStatus.OK);
    }
}
