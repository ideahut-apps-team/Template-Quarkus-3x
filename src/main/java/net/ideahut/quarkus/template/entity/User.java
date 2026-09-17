package net.ideahut.quarkus.template.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import net.ideahut.quarkus.annotation.Audit;
import net.ideahut.quarkus.entity.EntityAuditSoftDelete;
import net.ideahut.quarkus.generator.GeneratorOdtId;

@Audit
@Entity
@Table(
	name = "user_account", 
	indexes = { 
		@Index( name = "idx_user_account__username", columnList = "username")
	}
)
@Setter
@Getter
@SuppressWarnings("serial")
public class User extends EntityAuditSoftDelete {
	
	@Id
	@GeneratedValue(generator = GeneratorOdtId.NAME)
	@GeneratorOdtId
	@Column(name = "user_id", unique = true, nullable = false, length = 64)
	private String userId;
	
	@Column(name = "username", nullable = false, unique = true)
	private String username;
	
	@Column(name = "status", length = 1)
	private Character status;
	
	@Column(name = "description")
	private String description;
	
	
	@Transient
	private UserDetail detail;
	
	public User() {}
	
	public User(String userId) {
		this.userId = userId;
	}

}
