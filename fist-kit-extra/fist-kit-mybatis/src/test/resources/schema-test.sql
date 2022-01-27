/*
 *  Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 *  Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.gnu.org/licenses/lgpl.html
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

drop table if exists org_idx;
create table if not exists org_idx
(
    id bigint ,
    ancestor bigint ,
    descendant bigint ,
    distance int
);

drop table if exists book;
create table if not exists book
(
    id bigint identity primary key,
    title varchar
);
insert into book (id, title) values (90001, 'BOOK-900001');
insert into book (id, title) values (90002, 'BOOK-900002');
insert into book (id, title) values (90003, 'BOOK-900003');
insert into book (id, title) values (90004, 'BOOK-900004');
insert into book (id, title) values (90005, 'BOOK-900005');