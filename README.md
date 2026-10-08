# Ouvidoria MBFREIRE — Backend

Backend da aplicação **Ouvidoria MBFREIRE**, criado para registrar, acompanhar e gerenciar manifestações de clientes de forma segura e organizada.

## Objetivo

A aplicação permite que clientes registrem manifestações e acompanhem seus atendimentos, enquanto administradores gerenciam usuários, categorias, mensagens e encerramento das manifestações.

O sistema foi desenvolvido com foco em:

- segurança;
- controle de acesso;
- isolamento de dados entre clientes;
- rastreabilidade;
- proteção contra abuso.

## Perfis de acesso

### CLIENT

Pode:

- realizar login;
- alterar a senha no primeiro acesso;
- criar manifestações;
- visualizar apenas suas próprias manifestações;
- trocar mensagens;
- recuperar e alterar a senha.

### ADMIN

Pode:

- visualizar todas as manifestações;
- responder clientes;
- encerrar manifestações;
- cadastrar e gerenciar usuários;
- gerenciar categorias.

Não existe cadastro público.

## Principais funcionalidades

- Autenticação com JWT;
- Perfis `CLIENT` e `ADMIN`;
- Primeiro acesso com troca obrigatória de senha;
- Recuperação de senha por e-mail;
- Criação e acompanhamento de manifestações;
- Mensagens entre cliente e administrador;
- Encerramento de manifestações;
- Suporte opcional a anexos;
- Notificações por e-mail com Brevo;
- Rate limiting;
- Retenção automática de mensagens;
- Controle de acesso e isolamento entre clientes.

## Tecnologias

- Java 25
- Spring Boot 4.1.1
- Spring Security
- JWT
- Spring Data JPA
- PostgreSQL
- Flyway
- Docker
- Maven
- JUnit 5
- Mockito
- H2
- Brevo

## Segurança

A aplicação utiliza:

- JWT;
- autorização por roles;
- `tokenVersion` para revogação de sessões;
- rate limiting;
- validação de dados;
- isolamento por proprietário;
- rotas bloqueadas por padrão com `denyAll()`.

## Autor

Desenvolvido por **Arthur Novais Peixoto**.

GitHub: `arthurnovaisdev`