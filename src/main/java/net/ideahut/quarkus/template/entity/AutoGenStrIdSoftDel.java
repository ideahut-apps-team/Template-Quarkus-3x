package net.ideahut.quarkus.template.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.ideahut.quarkus.annotation.Audit;
import net.ideahut.quarkus.entity.EntityAuditSoftDelete;
import net.ideahut.quarkus.generator.GeneratorOdtId;

@Audit
@Entity
@Table(name = "auto_gen_str_id_soft_del")
@Setter
@Getter
@SuppressWarnings("serial")
public class AutoGenStrIdSoftDel extends EntityAuditSoftDelete {
	
	@Id
	@GeneratedValue(generator = GeneratorOdtId.NAME)
	@GeneratorOdtId
	@Column(name = "id", unique = true, nullable = false, length = 64)
	private String id;
	
	@Column(name = "name", nullable = false, length = 128)
	private String name;
	
	@Column(name = "description")
	private String description;

	@Column(name = "is_active", nullable = false, length = 1)
	private Character isActive;
	
	public AutoGenStrIdSoftDel() {}
	
	public AutoGenStrIdSoftDel(String id) {
		this.id = id;
	}
	
}
