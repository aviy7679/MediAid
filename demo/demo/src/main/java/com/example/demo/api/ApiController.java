package api;

import com.example.demo.bl.BusinessLogic;  // מחבר את שכבת ה-BL (לוגיקה עסקית)
import org.springframework.web.bind.annotation.*; // מספק את כל האנווטציות של Spring MVC

@RestController
@RequestMapping("/api")

public class ApiController {private final BusinessLogic businessLogic;

    public ApiController(BusinessLogic businessLogic) {
        this.businessLogic = businessLogic;
    }

    // GET - בקשה ללא פרמטרים
    @GetMapping("/message")
    public String getMessage() {
        return businessLogic.processData();
    }

    //  GET - בקשה עם פרמטרים
    @GetMapping("/message/{name}")
    public String getMessageWithName(@PathVariable String name) {
        return businessLogic.processDataWithName(name);
    }

    //  POST - בקשה שמקבלת נתונים מהלקוח
    @PostMapping("/message")
    public String postMessage(@RequestBody String newMessage) {
        return businessLogic.saveData(newMessage);
    }

}
