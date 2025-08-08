package com.example.spliteasyweb.model;

import jakarta.persistence.*;

@Entity @Table(name="persons",
  uniqueConstraints=@UniqueConstraint(columnNames={"name","group_id"}))
public class PersonEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false) private String name;
  @Column(name="group_id", nullable=false) private Long groupId;

  public Long getId(){ return id; }
  public String getName(){ return name; }
  public void setName(String name){ this.name = name; }
  public Long getGroupId(){ return groupId; }
  public void setGroupId(Long groupId){ this.groupId = groupId; }
}
