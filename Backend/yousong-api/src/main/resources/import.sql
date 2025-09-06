-- User: hugo / password
insert into benutzer(id, username, password) values
    (1, 'hugo', '$2a$10$MkLTGxdivqj427wEbGrwu.Qbx7G.2z.d31xVb1Qe9UCwimEdpqp1a')
    on conflict do nothing;
