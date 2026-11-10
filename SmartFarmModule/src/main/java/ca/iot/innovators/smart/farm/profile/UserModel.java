/** CENG-322-0CA
 * Chau Vi Cuong Nguyen  N01634611
 * Hoang Tran            N01613506
 * Truong An Nguyen      N01639800
 * Tuan Anh Pham         N01238459
 **/
package ca.iot.innovators.smart.farm.profile;

public class UserModel {
    private String userId;
    private String email;
    private String fullName;
    private String phone;
    private long createdAt;
    private String farmName;
    private String farmLocation;
    private String profileImageUrl;

    public UserModel() {}

    public UserModel(String userId, String email, String fullName, String phone,
                     String farmName, String farmLocation) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.createdAt = System.currentTimeMillis();
        this.farmName = farmName;
        this.farmLocation = farmLocation;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }
    public String getFarmLocation() { return farmLocation; }
    public void setFarmLocation(String farmLocation) { this.farmLocation = farmLocation; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}
