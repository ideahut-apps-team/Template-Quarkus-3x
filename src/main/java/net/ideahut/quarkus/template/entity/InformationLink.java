package net.ideahut.quarkus.template.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.ideahut.quarkus.annotation.Audit;
import net.ideahut.quarkus.entity.EntityAudit;
import net.ideahut.quarkus.generator.GeneratorOdtId;

@Audit
@Entity
@Table(name = "information_link")
@Setter
@Getter
@SuppressWarnings("serial")
public class InformationLink extends EntityAudit {

	@Id
	@GeneratedValue(generator = GeneratorOdtId.NAME)
	@GeneratorOdtId
	@Column(name = "link_id", unique = true, nullable = false, length = 64)
	private String linkId;
	
	@OnDelete(action = OnDeleteAction.CASCADE)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "information_id", nullable = false, foreignKey = @ForeignKey(name = "fk_information_link__information"))
	private Information information;
	
	@Column(name = "label", nullable = false, length = 100)
	private String label;
	
	@Column(name = "code", nullable = false, length = 16)
	private String code;
	
	@Lob
	//@Type(type = "org.hibernate.type.BinaryType")
	@Column(name = "target", nullable = false)
	private String target;
	
	@Column(name = "seqno", nullable = false)
	private Long seqno;
	
	public InformationLink() {}
	
	public InformationLink(String linkId) {
		this.linkId = linkId;
	}
	
}
