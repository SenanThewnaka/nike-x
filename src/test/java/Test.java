import com.zentora.nike_x.entity.Status;
import com.zentora.nike_x.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class Test {
    public static void main(String[] args) {
//        try (
                Session session = HibernateUtil.getSessionFactory().openSession();
//        ) {

//            // 2. Begin a Transaction
//            Transaction transaction = session.beginTransaction();
//
//            try {
//                // 3. Loop through your Enum types
//                for (Status.Type type : Status.Type.values()) {
//
//                    Status status = new Status();
//                    status.setType(String.valueOf(type)); // Or just status.setType(type) if your setter accepts the Enum directly
//
//                    // 4. SAVE the object
//                    session.persist(status); // or session.save(status) in older Hibernate versions
//                }
//
//                // 5. Commit the changes to the database
//                transaction.commit();
//                System.out.println("Statuses saved successfully!");
//
//            } catch (Exception e) {
//                // If something goes wrong, rollback
//                if (transaction != null) transaction.rollback();
//                e.printStackTrace();
//            }
//        }
    }
}
