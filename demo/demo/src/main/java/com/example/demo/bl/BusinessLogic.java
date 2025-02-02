package bl;

import com.example.demo.dal.DataRepository; // מחבר את שכבת ה-DAL
import org.springframework.stereotype.Service; // מסמן את המחלקה כשירות עסקי

@Service

public class BusinessLogic {
    private final DataRepository dataRepository;

    public BusinessLogic(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    public String processData() {
        return "Processed: " + dataRepository.getData();
    }

    public String processDataWithName(String name) {
        return "Hello, " + name + "! " + dataRepository.getData();
    }

    public String saveData(String newMessage) {
        dataRepository.saveData(newMessage);
        return "Data saved: " + newMessage;
    }
}
