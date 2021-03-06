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
create table org_idx
(
    id bigint ,
    ancestor bigint ,
    descendant bigint ,
    distance int
);

drop table if exists book;
create table book
(
    id bigint primary key,
    title varchar
);


drop table if exists people;
create table people
(
    id bigint primary key,
    name varchar
);