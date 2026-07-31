package com.fittrack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "image")
public class Image {

	@Id
	@Column(length = 36, nullable = false)
	private String id;

	@Column(nullable = false, unique = true)
	private String path;

	@Column(name = "content_base64")
	private String contentBase64;

	@Column(name = "content_type")
	private String contentType;

	@Column(name = "alt_text")
	private String altText;

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID().toString();
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getContentBase64() {
		return contentBase64;
	}

	public void setContentBase64(String contentBase64) {
		this.contentBase64 = contentBase64;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getAltText() {
		return altText;
	}

	public void setAltText(String altText) {
		this.altText = altText;
	}
}
