create table user_authorities (
	id int not null,
	created_at timestamp(6) null,
	authority_id int not null,
	username varchar(255) null,
	constraint user_authorities_pkey  primary key(id),
    constraint user_authority_user_fk foreign key(username)  references users(username),
    constraint user_authority_authority_fk foreign key(authority_id)  references authorities(id)
);