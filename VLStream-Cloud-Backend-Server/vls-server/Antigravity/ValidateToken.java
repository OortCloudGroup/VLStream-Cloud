import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;

public class ValidateToken {
    public static void main(String[] args) {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJpc3N1c2VyIiwiYXVkIjpbImF1ZGllbmNlIl0sInRlbmFudF9pZCI6IjAwMDAwMCIsInJvbGVfbmFtZSI6ImFkbWluaXN0cmF0b3IiLCJ1c2VyX2lkIjoiMTEyMzU5ODgyMTczODY3NTIwMSIsInJvbGVfaWQiOiIxMTIzNTk4ODE2NzM4Njc1MjAxIiwidXNlcl9uYW1lIjoiYWRtaW4iLCJ0b2tlbl90eXBlIjoiYWNjZXNzX3Rva2VuIiwiZGVwdF9pZCI6IjExMjM1OTg4MTM3Mzg2NzUyMDEiLCJhY2NvdW50IjoiYWRtaW4iLCJjbGllbnRfaWQiOiJzYWJlciIsImV4cCI6MTc4MjQ3NjU3NCwibmJmIjoxNzgyNDcyOTc0fQ.F2IhtvwpjHOUSm-IWv5XXBY5kdBjf3ACepxElq3T_Zs";
        String signKey = "TPbsGM3c2nzFWKwskmyo5flhXSUwb0PC";
        try {
            String base64Security = Base64.getEncoder().encodeToString(signKey.getBytes("UTF-8"));
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Security))).build()
                .parseSignedClaims(token)
                .getPayload();
            System.out.println("Token validation SUCCESS!");
            System.out.println("Claims: " + claims);
        } catch (Exception e) {
            System.out.println("Token validation FAILED!");
            e.printStackTrace();
        }
    }
}
